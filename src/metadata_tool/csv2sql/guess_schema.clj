(ns metadata-tool.csv2sql.guess-schema
  "Generate schema based on a table which contains many records."
  (:require [clojure.data.csv]
            [clojure.set :as set]
            [metadata-tool.csv2sql.dates :as dates]
            [metadata-tool.csv2sql.files :as files]
            [metadata-tool.csv2sql.util :as util]
            [metadata-tool.csv2sql.csvs :as csvs]))

(set! *warn-on-reflection* true)

(def ^:private strict-mode (atom false))

(defn setup-strict-mode
  [mode]
  (reset! strict-mode mode))

(def ^:dynamic *sql-types-and-parsers*
  ;; This data structure defines all of the SQL data types, and the appropriate
  ;; function to use when parsing a string containing that data type.
  ;; Parsers will be tried in sequential order, and the first one that works is used.
  ;; 
  ;; SQL               String->CLJ Parser
  [["NULL"             #(or (nil? %) (empty? %))]
   ["INTEGER"          #(Integer/parseInt %)]
   ["DOUBLE PRECISION" #(Float/parseFloat %)]
   ["DATE"             dates/parse-date]
   ["TIMESTAMPTZ"      dates/parse-RFC3339]
   ["TEXT"             #(str %)]]) ;; this is always true, so is the "default" value

(defn guess-sql-parser
  "Given an unknown string, this fn runs through all of the SQL types & parsers in 
  sql-types-and-parsers and returns the first row with a working parser."
  [string]
  (loop [types-and-parsers *sql-types-and-parsers*]
    (when-let [[sql-type parse-fn :as typerow] (first types-and-parsers)]
      (if (try (parse-fn string)
               (catch Exception e false))
        typerow
        (recur (next types-and-parsers))))))

(defn guess-all-sql-types-in-column
  "Like guess-sql-type, but an optimized version for looking at a whole column.
  In practice, this really reduces the number of tests and exceptions trapped
  over the simpler but much slower solution: 
         (set (flatten (map guess-sql-parser seq-of-strings)))"
  [seq-of-strings]
  (loop [strings seq-of-strings
         last-successful-parse-fn nil
         types-found #{}]
    (if-let [string (first strings)]
      (if (try (last-successful-parse-fn string)
               (catch Exception e false))
        (recur (next strings)  ; Previously successful parser worked again
               last-successful-parse-fn
               types-found)
        (if-let [[sql-type parse-fn] (guess-sql-parser string)]
          (recur (next strings)  ; A new working parser was found
                 parse-fn
                 (conj types-found sql-type))
          (recur (next strings)  ; No working parser found, move to next string
                 last-successful-parse-fn
                 (conj types-found nil))))
      types-found)))

(defn clean-column-names
  "Replaces whitespaces and periods in column names with underscores."
  [columns]
  (->> columns
       (map util/periods-to-underscores)
       (mapv util/spaces-to-underscores)
       (map util/camel-to-snake)))

(defn guess-csv-column-types
  "Returns a map of column name to the guessed SQL column type. Reads every
  row in the CSV, and returns all types found for each column. Works in 
  parallel and lazily on chunks of 1000 lines, to reduce the time to parse
  very large files."
  [csv-filepath]
  (println "Scanning:" csv-filepath)
  (with-open [reader (util/bom-reader csv-filepath)]
    (let [sep (csvs/guess-separator csv-filepath)
          rows (clojure.data.csv/read-csv reader :separator sep)
          header (clean-column-names (first rows))
          data-rows (rest rows)
          chunk-size 10000]
      (->> data-rows
           (partition-all chunk-size)
           (map #(apply map vector %)) ;; Convert list of rows into list of columns
           (map #(pmap guess-all-sql-types-in-column %))
           (map (fn [i data] (println (* 10000 (inc i)) "rows scanned") data) (range))
           (apply map (fn [& args] (reduce set/union args)))
           (map vector header)
           (into {})))))

(defn scan-csvdir-and-make-schema
  "Scans the header of every .csv file in CSVDIR, and returns a hashmap
  containing the schema of all the columns in the directory.
  If a non-alphanumeric string is found, raises an exception. 
  If the schema is inconsistent, raises an exception."
  [csvdir]
  (let [csv-schemas (->> (files/list-files-of-type csvdir "csv|tsv|txt")
                         (map guess-csv-column-types))
        columns (set (flatten (map keys csv-schemas)))
        problematic-columns (remove #(util/alphanumeric? % @strict-mode) columns)]
    (when (empty? csv-schemas)
      (throw (Exception. (str "Error: Not found any valid file(s) in " csvdir))))
    (when-not (empty? problematic-columns)
      (throw (Exception. (str "Non-alphanumeric characters found in column names:"
                              (apply str (interpose ", "  problematic-columns))))))
    (into {} (for [col columns]
               (let [all-types-for-col (->> (map #(vec (get % col)) csv-schemas)
                                            (flatten)
                                            (remove nil?)
                                            (set))
                     nullable-suffix (if (get all-types-for-col "NULL")
                                       " NULL"
                                       "")
                     types (disj all-types-for-col "NULL")]
                 (cond
                   (= 0 (count types))        [col nil]
                   (= 1 (count types))        [col (str (first types) nullable-suffix)]
                   ;; If it's mixed integer and float, make everything float
                   (= #{"INTEGER" "DOUBLE PRECISION"} types)
                   [col (str "DOUBLE PRECISION" nullable-suffix)]
                   ;; If the default type of TEXT is in there, choose text
                   (get types "TEXT") [col (str "TEXT" nullable-suffix)]
                   :else  ;; Otherwise we have some weird error
                   (throw (Exception. (str "Inconsistent types across files for column: "
                                           col (vec types))))))))))

(defn drop-trailing-null
  [s]
  (if (= " NULL" (apply str (take-last 5 s)))
    (apply str (drop-last 5 s))
    s))

(defn parse-csv-rows-using-schema
  "Lazily parse CSV-ROWS using the schema."
  [schema csv-rows]
  (let [header (clean-column-names (first csv-rows))
        types  (map #(drop-trailing-null (get schema %)) header)
        empty-string-to-nil (fn [s] (if (and (string? s) (empty? s)) nil s))
        raw-rows (map #(map empty-string-to-nil %) (rest csv-rows))
        all-parsers (into {} *sql-types-and-parsers*)
        row-parsers (mapv #(get all-parsers %) types)
        process-row (fn [raw-row]
                      (loop [raw-row raw-row
                             parsers row-parsers
                             row []]
                        (if-not (empty? parsers)
                          (let [parse-fn (first parsers)
                                element (first raw-row)]
                            (if parse-fn
                              (recur (next raw-row)
                                     (next parsers)
                                     (conj row (when-not (empty? element)
                                                 (try (parse-fn element)
                                                      (catch Exception e
                                                        (println "Schema:" schema)
                                                        (println "Header:" header)
                                                        (println "Raw row:" raw-row)
                                                        (throw e))))))
                              (recur (next raw-row) ;; Skip null parse-fns
                                     (next parsers)
                                     row)))
                          row)))
        typed-rows (map process-row raw-rows)
        cnt (atom 0)
        chunk-size 1000
        header-with-no-nils (->> (map vector row-parsers header)
                                 (remove #(nil? (first %)))
                                 (mapv second))]
    [header-with-no-nils typed-rows]))

(defn table-definition-sql-string
  "Returns a string suitable for creating a SQL table named TABLE-NAME, given
  a hashmap SCHEMA of column names to column types. The ENDING-STRING is appended
  to the end of the create table statement, if needed. "
  [table-name schema & [ending-string]]
  (let [ending-string (or ending-string "")
        col-defs (->> schema
                      (sort-by first)
                      (remove (comp nil? second))
                      (map (fn [[col type]] (format "\t%s %s" col type)))
                      (interpose ",\n")
                      (apply str))]
    (format "CREATE TABLE %s (\n%s %s\n);"
            table-name col-defs ending-string)))

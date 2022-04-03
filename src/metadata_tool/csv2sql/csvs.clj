(ns metadata-tool.csv2sql.csvs
  "Functions for loading and saving CSVs."
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.string :as clj-str]
            [metadata-tool.csv2sql.util :as util]))

(set! *warn-on-reflection* true)

(defn empty-string-to-nil
  "Returns a nil if given an empty string S, otherwise returns S."
  [s]
  (if (and (string? s) (empty? s))
    nil
    s))

(defn guess-separator
  [filepath]
  (with-open [reader (util/bom-reader filepath)]
    (let [header (first (line-seq reader))
          seps [\tab \, \; \space]
          sep-map (->> (map #(hash-map % (count (clj-str/split header (re-pattern (str %))))) seps)
                       (into {}))]
      (key (apply max-key val sep-map)))))

(defn dissoc-nils
  "Drops keys with nil values, or nil keys, from the hashmap H."
  [h]
  (into {} (filter (fn [[k v]] (and v k)) h)))


(defn load-csv
  "Returns a data structure loaded from a CSV file at FILEPATH."
  [filepath]
  (with-open [reader (util/bom-reader filepath)]
    (->> (csv/read-csv reader)
         (map (fn [row] (map empty-string-to-nil row)))
         (doall))))

(defn save-csv
  "Saves a vector of vectors DATA (i.e. a CSV) to disk at FILEPATH. "
  [vec-of-vecs filepath]
  (with-open [writer (io/writer filepath)]
    (csv/write-csv writer vec-of-vecs)))

(defn tabular->maps
  "Converts a vector of vectors into a vector of maps. Assumes that the
  first row of the CSV is a header that contains column names."
  [tabular]
  (let [header (first tabular)]
    (-> (map zipmap (repeat header) (rest tabular))
        (mapv dissoc-nils))))

(defn maps->tabular
  "Converts a vector of vectors into a vector of maps."
  [rowmaps]
  (let [columns (vec (sort (into #{} (map name (flatten (map keys rowmaps))))))]
    (vec (conj (for [row rowmaps]
                 (vec (for [col columns]
                        (str (get row col "")))))
               columns))))

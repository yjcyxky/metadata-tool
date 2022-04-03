(ns metadata-tool.csv2sql.json
  "Functions for loading and saving JSONs."
  (:require [clojure.data.json :as js]
            [clojure.java.io :as io]
            [metadata-tool.csv2sql.files :as files]
            [metadata-tool.csv2sql.csvs :as csv]
            [metadata-tool.csv2sql.util :as util]))

(set! *warn-on-reflection* true)

(defn load-json
  "Returns a data structure loaded from a CSV file at FILEPATH."
  [filepath]
  (with-open [reader (util/bom-reader filepath)]
    (js/read reader)))

(defn save-json
  "Saves a vector of vectors DATA (i.e. a CSV) to disk at FILEPATH. "
  [data filepath]
  (with-open [writer (io/writer filepath)]
    (js/write data writer)))

(defn flatten-keys
  "Given a possibly nested hashmap, flattens it and prepends the names of parent
  keys to child key names, seperated by periods. {A {b 1, c 2}} -> {A.b 1, A.c 2}"
  [h & [prefix]]
  (let [prefix (or prefix [])]
    (loop [h h
           ret {}]
      (if-let [kv (first h)]
        (let [[k v] kv]
          (if (map? v)
            (recur (rest h)
                   (merge ret (flatten-keys v (conj prefix k))))
            (recur (rest h)
                   (assoc ret (if (empty? prefix)
                                k
                                (apply str (interpose "." (conj prefix k))))
                          v))))
        ret))))

(defn flat-hashmap-to-tabular
  "Takes an (assumed flat) hashmap, and converts it to tabular format with a single row.
  that is in the same format as a CSV."
  [h]
  (let [columns (vec (sort (keys h)))
        row (mapv #(str (get h % "")) columns)]
    [columns row]))

(defn load-json-as-csv
  "Loads a JSON as if it were a CSV."
  [^String filepath]
  (->> (load-json filepath)
       (flatten-keys)
       (flat-hashmap-to-tabular)))

(defn convert-jsons-to-csvs!
  "Scans through the subdirectories of CSVDIR, and for each JSON that exists,
  creates a .csv file (if it does not already exist) with the same contents."
  [csvdir]
  (printf "Converting JSONs into CSVs: %s\n" csvdir)
  (flush)
  (doseq [dir (files/list-subdirectories csvdir)]
    (doseq [jsonfile (files/list-files-of-type dir "json")]
      (let [csvfile ^java.io.File (util/matching-csv-for-json jsonfile)]
        (when-not (.isFile csvfile)
          (let [filepath (.getAbsolutePath ^java.io.File jsonfile)]
            (try (-> filepath
                     (load-json-as-csv)
                     (csv/save-csv csvfile))
                 (catch Exception e
                   (println "ERROR LOADING: " filepath)))))))))

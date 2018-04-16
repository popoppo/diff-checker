(ns diff-checker.core
  (:require
   [clj-http.client :as http-cli]
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.spec.alpha :as s]
   [clojure.tools.cli :as tcli]
   [environ.core :as environ]
   [net.cgrand.enlive-html :as enlive])
  (:import java.security.MessageDigest
           java.math.BigInteger)
  (:gen-class))


;; Spec

(s/def ::name string?)
(s/def ::target map?)
(s/def ::updated string?) ;; should be Date
(s/def ::contents-hash string?)
(s/def ::task (s/keys :req-un [::name ::target]
                      :opt-un [::updated ::contents-hash]))

(s/fdef create-task
        :args (s/cat :name string?
                     :target map?)
        :ret integer?)

(s/fdef register-task
        :args (s/cat :task ::task))


;; Implementations

(defn create-task
  [name target]
  {:name name
   :target target})

;; Thanks: https://gist.github.com/jizhang/4325757
(defn md5 [^String s]
  (let [algorithm (MessageDigest/getInstance "MD5")
        raw (.digest algorithm (.getBytes s))]
    (format "%032x" (BigInteger. 1 raw))))

(defn calc-cotents-hash
  "Using MD5"
  [contents]
  (md5 contents))

(defn fetch-contents
  [target-map]
  (let [url (:url target-map)
        selector (as-> (:selector target-map) <>
                   (into [] (map keyword <>)))
        resource (-> url io/reader enlive/html-resource)]
    (-> resource
        (enlive/select selector)
        first)))

(defn stringnize-html-resource
  [resource]
  (->> (map #(str %) resource)
       (clojure.string/join "")))

(defn get-contents-hash
  [target-map]
  (-> target-map
      fetch-contents
      stringnize-html-resource
      calc-cotents-hash))

(defn get-dir-path
  []
  (str (System/getenv "HOME") "/.diff-checker"))

(defn get-file-path
  [task-name]
  (str (get-dir-path) "/" task-name))

(defn write-task-to-file
  [task file-path]
  (with-open [w (io/writer file-path)]
    (.write w (json/write-str task))))

(defn slack-notifier
  [param-map]
  (let [body {:body (str "{\"channel\": \"#market\",
                           \"username\": \"webhookbot\",
                           \"text\": \"" "test" "\"}")}]
    (http-cli/post (environ/env :slack-url) body)))

(defn handle-record
  [record & [notifier]]
  (let [target-map (:target record)
        chash (get-contents-hash target-map)]
    (when-not (= chash (:contents-hash record))
      (let [new-rec (assoc-in record [:target :contents-hash] chash)
            fpath (get-file-path (:name record))
            f (or notifier prn)]
        (write-task-to-file new-rec fpath)
        (f new-rec)))))

(defn read-file-as-json
  [fpath]
  (-> fpath
      slurp
      (json/read-str :key-fn keyword)))

(defn check-diffs
  []
  (let [fobj (io/file (get-dir-path))]
    (if-not (.exists fobj)
      "Task not found. Please register tasks first"
      (let [fseq (file-seq fobj)]
        (map #(and
               (not (.isDirectory %))
               (-> (.getAbsolutePath %)
                   read-file-as-json
                   handle-record))
             fseq)))))


(defn list-tasks
  []
  (let [dir-path (get-dir-path)
        dobj (io/file dir-path)
        files (file-seq dobj)]
    (->> files
         (remove #(not (.isFile %)))
         (map #(.getName %)))))


(defn get-task-info
  [task-name]
  (let [fpath (get-file-path task-name)]
    (read-file-as-json fpath)))


(def cli-options
  [["-l" "--list" "List tasks"]
   ["-n" "--name Name" "Task name"]]
  #_[["-u" "--url URL" "Target URL"]
   ["-s" "--selector SELECTOR" "Enlive selector"]])


(defn -main
  [& args]
  ;; TODO: options, list and register task
  (let [{:keys [options arguments summary errors]} (tcli/parse-opts args cli-options)]
    (cond
      (:list options) (-> list-tasks prn)
      (:name options) (-> (get-task-info (:name options)) prn)
      :else (check-diffs))))


;; Utils

(defn register-task
  [task] ;; might support DB or other storages
  ;; existance check sould be done?
  (let [fpath (get-file-path (:name task))]
    (if-not (.exists (io/file fpath))
      (io/make-parents fpath))
    (write-task-to-file task fpath)))


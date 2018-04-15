(ns cmd.get-hash
  (:require
   [diff-checker.core :as dc]))

(defn -main [& args]
  (prn args)
  (prn (dc/get-contents-hash {:url (first args) :selector (second args)})))


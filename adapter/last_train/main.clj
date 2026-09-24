(ns last-train.main
  "Process entry point: exits on bad options. Kept out of src/ so test tools skip it."
  (:require [last-train.cli :as cli]))

(defn -main [& args]
  (let [{:keys [error puzzle]} (cli/parse-args args)]
    (if error
      (do (binding [*out* *err*] (println error))
          (System/exit 2))
      (cli/play puzzle))))

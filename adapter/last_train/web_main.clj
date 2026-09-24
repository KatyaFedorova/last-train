(ns last-train.web-main
  "Web server entry point: serves the browser game over HTTP. Kept out of src/
  so test tools skip it."
  (:require [last-train.puzzles :as puzzles]
            [last-train.web :as web]
            [org.httpkit.server :as http]))

(defn- handle [{:keys [request-method uri body]}]
  (web/respond puzzles/reference {:request-method request-method :uri uri :body (some-> body slurp)}))

(defn -main [& args]
  (let [{:keys [error port]} (web/parse-args args)]
    (if error
      (do (binding [*out* *err*] (println error))
          (System/exit 2))
      (do (http/run-server handle {:port port})
          (println (str "LAST TRAIN is running at http://localhost:" port "/"))
          @(promise)))))

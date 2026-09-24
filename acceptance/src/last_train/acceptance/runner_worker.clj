(ns last-train.acceptance.runner-worker
  "Persistent APS runner adapter for gherkin-mutator: reads newline-delimited
  JSON jobs on stdin, runs every scenario execution of the job's IR through
  the acceptance runtime and step handlers, and writes one JSON response per job."
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [last-train.acceptance.runtime :as runtime]))

(defn- run-ir [step-handlers ir]
  (vec (keep (fn [{:keys [scenario example-index]}]
               (runtime/run-execution step-handlers ir scenario example-index))
             (runtime/executions ir))))

(defn run-job [step-handlers job]
  (let [started (System/nanoTime)
        response (try
                   (let [failures (run-ir step-handlers (runtime/read-ir (slurp (get job "feature_json"))))]
                     {"outcome" (if (seq failures) "test_failure" "test_success")
                      "output" (str/join "\n" failures)
                      "error" ""})
                   (catch Exception e
                     {"outcome" "infrastructure_error" "output" "" "error" (str e)}))]
    (assoc response
           "id" (get job "id")
           "duration" (- (System/nanoTime) started))))

(defn serve [step-handlers]
  (doseq [line (line-seq (java.io.BufferedReader. *in*))
          :when (not (str/blank? line))]
    (let [response (try
                     (run-job step-handlers (json/parse-string line))
                     (catch Exception e
                       {"id" "" "outcome" "infrastructure_error" "output" "" "error" (str e) "duration" 0}))]
      (println (json/generate-string response))
      (flush))))

(defn -main [& _]
  (require 'last-train.acceptance.steps)
  (serve @(resolve 'last-train.acceptance.steps/handlers)))

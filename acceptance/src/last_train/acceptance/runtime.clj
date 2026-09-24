(ns last-train.acceptance.runtime
  "APS acceptance runtime: expands IR scenarios into executions and dispatches
  each step to the first step handler whose regex matches its text.
  A step handler is [regex (fn [world & captures] world')]. Captures written as
  <name> are resolved from the current example row."
  (:require [cheshire.core :as json]))

(defn read-ir [json-text]
  (json/parse-string json-text))

(defn check
  "Fail the current step with message unless ok."
  [ok message]
  (when-not ok
    (throw (ex-info message {::failure true}))))

(defn executions [ir]
  (let [background (get ir "background" [])]
    (vec (for [[scenario-index scenario] (map-indexed vector (get ir "scenarios"))
               [example-index example] (map-indexed vector (or (seq (get scenario "examples")) [{}]))]
           {:scenario scenario-index
            :example-index example-index
            :name (str (get scenario "name") "/example_" (inc example-index))
            :example example
            :steps (into (vec background) (get scenario "steps"))}))))

(defn- resolve-capture [example capture]
  (if-let [[_ placeholder] (and capture (re-matches #"<([A-Za-z0-9_]+)>" capture))]
    (or (get example placeholder)
        (check false (str "Missing example value: " placeholder)))
    capture))

(defn- run-step [step-handlers world example step]
  (let [text (get step "text")
        matches (keep (fn [[pattern handler]]
                        (when-let [found (re-matches pattern text)]
                          [handler (if (string? found) [] (rest found))]))
                      step-handlers)]
    (check (seq matches) (str "Unsupported step: " text))
    (check (= 1 (count matches)) (str "Ambiguous step: " text))
    (let [[handler captures] (first matches)
          values (mapv #(resolve-capture example %) captures)]
      (try
        (apply handler world values)
        (catch clojure.lang.ExceptionInfo e
          (if (::failure (ex-data e))
            (throw (ex-info (str (ex-message e) "\n  in step: " text) (ex-data e)))
            (throw e)))
        (catch Exception e
          (throw (ex-info (str "Error in step: " text "\n  " e) {::failure true})))))))

(defn run-execution
  "Run one scenario execution with a fresh world. Returns nil on success or a
  failure description."
  [step-handlers ir scenario-index example-index]
  (let [{:keys [name example steps]}
        (first (filter #(and (= scenario-index (:scenario %)) (= example-index (:example-index %)))
                       (executions ir)))]
    (try
      (reduce #(run-step step-handlers %1 example %2) {} steps)
      nil
      (catch clojure.lang.ExceptionInfo e
        (if (::failure (ex-data e))
          (str name ": " (ex-message e))
          (str name ": " e)))
      (catch Exception e
        (str name ": " e)))))

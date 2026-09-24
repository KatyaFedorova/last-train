(ns last-train.check
  "Runs a test.check property and prints the shrunk counterexample on failure."
  (:require [clojure.test.check :as tc]))

(defn passes? [property]
  (let [result (tc/quick-check 300 property)]
    (when-not (:pass? result) (prn (:shrunk result)))
    (:pass? result)))

(ns last-train.logic-property-spec
  (:require [speclj.core :refer :all]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [last-train.generators :as g]
            [last-train.logic :as logic]))

(defn- passes? [property]
  (let [result (tc/quick-check 300 property)]
    (when-not (:pass? result) (prn (:shrunk result)))
    (:pass? result)))

(describe "Logic engine properties"
  (it "never rules out the world the facts were stated in"
    (should (passes? (prop/for-all [[w facts] (gen/bind g/world #(gen/tuple (gen/return %) (gen/vector (g/fact %) 0 6)))]
                       (some #{w} (logic/consistent facts))))))

  (it "narrows the same way whatever order facts arrive in"
    (should (passes? (prop/for-all [[w facts] (gen/bind g/world #(gen/tuple (gen/return %) (gen/vector (g/fact %) 0 6)))]
                       (= (set (logic/consistent facts))
                          (set (reduce (fn [worlds f] (logic/consistent [f] worlds)) logic/all-worlds (reverse facts))))))))

  (it "only narrows as facts accumulate"
    (should (passes? (prop/for-all [[w facts] (gen/bind g/world #(gen/tuple (gen/return %) (gen/vector (g/fact %) 1 6)))]
                       (every? (set (logic/consistent (butlast facts))) (logic/consistent facts))))))

  (it "has the Agent and the Awake passenger answer every proposition oppositely"
    (should (passes? (prop/for-all [w g/world p g/prop]
                       (let [seat-of (fn [r] (first (filter #(= r (w %)) logic/seats)))]
                         (not= (logic/can-say? w (seat-of :agent) p)
                               (logic/can-say? w (seat-of :awake) p)))))))

  (it "has a Sleeper answer the same in every world"
    (should (passes? (prop/for-all [p g/prop w1 g/world w2 g/world]
                       (let [sleeper-in (fn [w] (first (filter #(= :sleeper (w %)) logic/seats)))]
                         (= (logic/can-say? w1 (sleeper-in w1) p)
                            (logic/can-say? w2 (sleeper-in w2) p)))))))

  (it "treats negation as the opposite answer"
    (should (passes? (prop/for-all [w g/world speaker g/seat p g/prop]
                       (not= (logic/can-say? w speaker p) (logic/can-say? w speaker [:not p])))))))

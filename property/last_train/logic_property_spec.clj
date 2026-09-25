(ns last-train.logic-property-spec
  (:require [speclj.core :refer :all]
            [last-train.check :refer [passes?]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [last-train.generators :as g]
            [last-train.logic :as logic]))

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

  (it "has the Agent and a human answer every proposition oppositely"
    (should (passes? (prop/for-all [w g/world p g/prop]
                       (let [agent (logic/agent-seat w)
                             human (first (remove #{agent} logic/seats))]
                         (not= (logic/can-say? w agent p)
                               (logic/can-say? w human p)))))))

  (it "treats negation as the opposite answer"
    (should (passes? (prop/for-all [w g/world speaker g/seat p g/prop]
                       (not= (logic/can-say? w speaker p) (logic/can-say? w speaker [:not p])))))))

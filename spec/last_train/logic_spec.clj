(ns last-train.logic-spec
  (:require [speclj.core :refer :all]
            [last-train.logic :as logic]))

(def wa {:A :agent :B :human :C :human :D :human})
(def wb {:A :human :B :agent :C :human :D :human})
(def wd {:A :human :B :human :C :human :D :agent})

(def reference-opening
  [[:A [:is :B :agent] true]
   [:B [:is :A :agent] true]
   [:C [:is :C :agent] false]
   [:D [:is :C :agent] false]])
(def reference-worlds (logic/consistent reference-opening))

(describe "Worlds"
  (it "has four worlds, one per Agent seat, everyone else human"
    (should= 4 (count (set logic/all-worlds)))
    (doseq [world logic/all-worlds]
      (should= {:agent 1 :human 3} (frequencies (vals world)))))

  (it "finds the Agent seat of a world"
    (should= :A (logic/agent-seat wa))
    (should= :D (logic/agent-seat wd)))

  (it "lists the distinct Agent seats of worlds in seat order"
    (should= [:A :B :D] (logic/agent-seats [wd wa wb wa]))))

(describe "Proposition evaluation"
  (it "evaluates Is"
    (should (logic/evaluate [:is :A :agent] wa))
    (should-not (logic/evaluate [:is :B :agent] wa))
    (should (logic/evaluate [:is :D :human] wa)))

  (it "evaluates Not, And and Or"
    (should (logic/evaluate [:not [:is :B :agent]] wa))
    (should (logic/evaluate [:and [:is :A :agent] [:is :D :human]] wa))
    (should-not (logic/evaluate [:and [:is :A :agent] [:is :B :agent]] wa))
    (should (logic/evaluate [:or [:is :B :agent] [:is :A :agent]] wa))
    (should-not (logic/evaluate [:or [:is :B :agent] [:is :C :agent]] wa)))

  (it "rejects an unknown proposition"
    (should-throw (logic/evaluate [:maybe :A] wa))))

(describe "What a passenger can say"
  (it "lets a human assert only truths"
    (should (logic/can-say? wa :D [:is :A :agent]))
    (should-not (logic/can-say? wa :D [:is :B :agent])))

  (it "lets the Agent assert only falsehoods"
    (should-not (logic/can-say? wa :A [:is :A :agent]))
    (should (logic/can-say? wa :A [:is :B :agent]))))

(describe "Consistent worlds"
  (it "keeps every world when there are no facts"
    (should= logic/all-worlds (logic/consistent [])))

  (it "leaves Vera and Tomasz as suspects after the reference opening"
    (should= #{wa wb} (set reference-worlds)))

  (it "narrows a given world set with answered facts"
    (should= [wa] (logic/consistent [[:C [:is :A :agent] true]] reference-worlds))))

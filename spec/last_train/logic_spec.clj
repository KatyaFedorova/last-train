(ns last-train.logic-spec
  (:require [speclj.core :refer :all]
            [last-train.logic :as logic]))

(def w1 {:A :agent :B :sleeper :C :sleeper :D :awake})
(def w2 {:A :sleeper :B :awake :C :agent :D :sleeper})
(def w3 {:A :sleeper :B :awake :C :sleeper :D :agent})

(def reference-opening
  [[:A [:count-eq :agent 0] true]
   [:B [:not [:is :A :agent]] true]
   [:C [:same :B :C] true]
   [:D [:same :B :C] true]])

(describe "Worlds"
  (it "has twelve distinct worlds of one Agent, one Awake and two Sleepers"
    (should= 12 (count (set logic/all-worlds)))
    (doseq [world logic/all-worlds]
      (should= {:agent 1 :awake 1 :sleeper 2} (frequencies (vals world)))))

  (it "finds the Agent seat of a world"
    (should= :A (logic/agent-seat w1))
    (should= :D (logic/agent-seat w3)))

  (it "lists the distinct Agent seats of worlds in seat order"
    (should= [:A :C :D] (logic/agent-seats [w3 w1 w2 w1]))))

(describe "Proposition evaluation"
  (it "evaluates Is"
    (should (logic/evaluate [:is :A :agent] w1))
    (should-not (logic/evaluate [:is :B :agent] w1))
    (should (logic/evaluate [:is :D :awake] w1)))

  (it "evaluates Same"
    (should (logic/evaluate [:same :B :C] w1))
    (should-not (logic/evaluate [:same :A :B] w1)))

  (it "evaluates CountEq"
    (should (logic/evaluate [:count-eq :sleeper 2] w1))
    (should-not (logic/evaluate [:count-eq :agent 0] w1)))

  (it "evaluates Not, And and Or"
    (should (logic/evaluate [:not [:is :B :agent]] w1))
    (should (logic/evaluate [:and [:is :A :agent] [:is :D :awake]] w1))
    (should-not (logic/evaluate [:and [:is :A :agent] [:is :B :awake]] w1))
    (should (logic/evaluate [:or [:is :B :agent] [:is :A :agent]] w1))
    (should-not (logic/evaluate [:or [:is :B :agent] [:is :C :agent]] w1)))

  (it "rejects an unknown proposition"
    (should-throw (logic/evaluate [:maybe :A] w1))))

(describe "What a passenger can say"
  (it "lets the Awake passenger assert only truths"
    (should (logic/can-say? w1 :D [:is :A :agent]))
    (should-not (logic/can-say? w1 :D [:is :B :agent])))

  (it "lets the Agent assert only falsehoods"
    (should-not (logic/can-say? w1 :A [:is :A :agent]))
    (should (logic/can-say? w1 :A [:is :B :agent])))

  (it "lets a Sleeper assert what is true when everyone is a Sleeper"
    (should-not (logic/can-say? w1 :B [:is :A :agent]))
    (should (logic/can-say? w1 :B [:is :A :sleeper]))
    (should (logic/can-say? w1 :B [:same :A :D]))
    (should (logic/can-say? w1 :B [:count-eq :agent 0]))))

(describe "Consistent worlds"
  (it "keeps every world when there are no facts"
    (should= logic/all-worlds (logic/consistent [])))

  (it "leaves the three reference worlds after the reference opening"
    (should= #{w1 w2 w3} (set (logic/consistent reference-opening)))
    (should= 3 (count (logic/consistent reference-opening))))

  (it "narrows a given world set with answered facts"
    (let [worlds (logic/consistent reference-opening)]
      (should= [w1 w2] (logic/consistent [[:B [:is :D :agent] false]] worlds)))))

(describe "Questions"
  (it "offers 72 legal questions"
    (should= 72 (count logic/questions))
    (should= 72 (count (set logic/questions))))

  (it "asks each speaker about each seat and role, and each pair of seats"
    (should-contain [:B [:is :D :agent]] logic/questions)
    (should-contain [:A [:same :C :D]] logic/questions)
    (should-not-contain [:A [:same :D :C]] logic/questions)))

(describe "Solvability"
  (let [worlds (logic/consistent reference-opening)]
    (it "is solved when every world has the same Agent"
      (should (logic/solvable? [w1] 0)))

    (it "is not solved at depth zero with several Agent candidates"
      (should-not (logic/solvable? worlds 0)))

    (it "is not solvable with one question for the reference puzzle"
      (should-not (logic/solvable? worlds 1)))

    (it "is solvable with two adaptive questions for the reference puzzle"
      (should (logic/solvable? worlds 2)))))

(ns last-train.puzzles-spec
  (:require [speclj.core :refer :all]
            [clojure.string :as str]
            [last-train.logic :as logic]
            [last-train.puzzles :as puzzles]))

(def reserved-words #{"a" "b" "c" "d" "i" "me" "you"})

(defn- puzzles-from [n level]
  (map first (take n (iterate (fn [[_ s]] (puzzles/generate s level))
                              (puzzles/generate (puzzles/seed n) level)))))

(describe "Personas"
  (it "have distinct names that are not seat letters or pronouns"
    (let [names (map (comp str/lower-case :name) puzzles/personas)]
      (should= (count names) (count (set names)))
      (should-not (some reserved-words names)))))

(describe "Seeds"
  (it "turn any integer into a generator seed"
    (doseq [n [0 1 42 -7 999999999]]
      (should (< 0 (puzzles/seed n) 2147483647))))

  (it "replay the same puzzle"
    (should= (puzzles/generate (puzzles/seed 42) 1) (puzzles/generate (puzzles/seed 42) 1)))

  (it "differ for different numbers"
    (should-not= (first (puzzles/generate (puzzles/seed 1) 1)) (first (puzzles/generate (puzzles/seed 2) 1)))))

(describe "Generated puzzles"
  (it "leave exactly one possible Agent, the true one"
    (doseq [level [1 2] puzzle (puzzles-from 60 level)]
      (should (puzzles/valid? puzzle))
      (should= [(:true-world puzzle)] (logic/consistent (:opening puzzle)))))

  (it "give four different passengers one line each"
    (doseq [puzzle (puzzles-from 30 1)]
      (should= logic/seats (map first (:opening puzzle)))
      (should= 4 (count (set (map :name (vals (:personas puzzle))))))))

  (it "use only 'is the Agent' lines on level 1 and at least one 'or' line on level 2"
    (doseq [puzzle (puzzles-from 30 1)]
      (should (every? (fn [[_ [op]]] (= :is op)) (:opening puzzle))))
    (doseq [puzzle (puzzles-from 30 2)]
      (should (some (fn [[_ [op]]] (= :or op)) (:opening puzzle)))))

  (it "put the Agent in every seat"
    (should= logic/seats (logic/agent-seats (map :true-world (puzzles-from 60 1))))))

(describe "Puzzle rules"
  (it "reject a puzzle that leaves two suspects"
    (should-not (puzzles/valid? {:true-world (puzzles/world :A)
                                 :opening [[:A [:is :B :agent] true]
                                           [:B [:is :A :agent] true]
                                           [:C [:is :C :agent] false]
                                           [:D [:is :D :agent] false]]})))

  (it "reject a puzzle whose true world contradicts its opening"
    (should-not (puzzles/valid? {:true-world (puzzles/world :B)
                                 :opening [[:A [:is :B :agent] true]
                                           [:B [:is :A :agent] true]
                                           [:C [:is :A :agent] true]
                                           [:D [:is :D :agent] false]]})))

  (it "accept a puzzle with one suspect"
    (should (puzzles/valid? {:true-world (puzzles/world :A)
                             :opening [[:A [:is :B :agent] true]
                                       [:B [:is :A :agent] true]
                                       [:C [:is :A :agent] true]
                                       [:D [:is :D :agent] false]]})))

  (it "start with three easy trains"
    (should= [1 1 1 2 2 2 2 2 2 2] (map puzzles/level (range 1 11)))))

(ns last-train.puzzles-spec
  (:require [speclj.core :refer :all]
            [clojure.string :as str]
            [last-train.logic :as logic]
            [last-train.puzzles :as puzzles]))

(def reserved-words #{"a" "b" "c" "d" "i" "me" "you"})

(defn- with-opening [puzzle opening]
  (assoc puzzle :opening opening))

(describe "Puzzle catalog"
  (it "has at least five uniquely named puzzles including the reference"
    (let [names (map :name puzzles/catalog)]
      (should (<= 5 (count names)))
      (should= (count names) (count (set names)))
      (should-contain "reference" names)))

  (it "finds puzzles by name"
    (should= puzzles/reference (puzzles/by-name "reference"))
    (should-be-nil (puzzles/by-name "nope"))
    (should-be-nil (puzzles/by-name nil)))

  (it "keeps the reference puzzle's Agent and personas"
    (should= {:A :agent :B :human :C :human :D :human} (:true-world puzzles/reference))
    (should= "Mr. Grey" (get-in puzzles/reference [:personas :D :name])))

  (it "only ships puzzles that follow the puzzle rules"
    (doseq [puzzle puzzles/catalog]
      (should (puzzles/valid? puzzle))))

  (it "gives every passenger a distinct name that is not a seat letter or pronoun"
    (doseq [{:keys [personas]} puzzles/catalog]
      (let [names (map (comp str/lower-case :name) (vals personas))]
        (should= 4 (count (set names)))
        (should-not (some reserved-words names)))))

  (it "puts the Agent in every seat somewhere in the catalog"
    (should= logic/seats (logic/agent-seats (map :true-world puzzles/catalog)))))

(describe "Puzzle rules"
  (it "accepts the reference puzzle"
    (should (puzzles/valid? puzzles/reference)))

  (it "rejects a puzzle whose true world contradicts its opening"
    (should-not (puzzles/valid? (assoc puzzles/reference :true-world
                                       {:A :human :B :human :C :human :D :agent}))))

  (it "rejects a puzzle already solved at boarding"
    (let [opening [[:A [:is :B :agent] true]
                   [:B [:is :A :agent] true]
                   [:C [:is :A :agent] true]
                   [:D [:is :D :agent] false]]]
      (should= [:A] (logic/agent-seats (logic/consistent opening)))
      (should-not (puzzles/valid? (with-opening puzzles/reference opening)))))

  (it "rejects a puzzle that leaves every passenger a suspect"
    (let [opening (vec (for [seat logic/seats] [seat [:is seat :agent] false]))]
      (should= 4 (count (logic/consistent opening)))
      (should-not (puzzles/valid? (with-opening puzzles/reference opening))))))

(describe "Picking a puzzle"
  (it "never repeats the puzzle just played"
    (doseq [draw (range 50)]
      (should-not= "reference" (:name (puzzles/pick (fn [n] (mod draw n)) "reference")))))

  (it "can pick any puzzle when nothing was played"
    (should= (set (map :name puzzles/catalog))
             (set (for [draw (range (count puzzles/catalog))]
                    (:name (puzzles/pick (fn [_] draw) nil)))))))

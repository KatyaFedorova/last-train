(ns last-train.puzzles
  "Seeded puzzle generator. A puzzle is four passengers, A to D, and one line each;
  the lines alone must leave exactly one possible Agent. Seeds thread through
  a Park-Miller generator so a run replays identically in Clojure and
  ClojureScript."
  (:require [last-train.logic :as logic]))

(def ^:private modulus 2147483647)

(defn- next-seed [s] (mod (* 16807 s) modulus))

(defn seed
  "A generator seed from any integer, stirred so small numbers start anywhere."
  [n]
  (nth (iterate next-seed (inc (mod n (dec modulus)))) 3))

(defn- draw
  "[a number 0..n-1, the next seed]."
  [s n]
  [(quot (* s n) modulus) (next-seed s)])

(defn- agent [seat] [:is seat :agent])

(defn- lines-for
  "Lines speaker could say about the others; level 2 adds 'X or Y is the Agent'."
  [speaker level]
  (let [others (remove #{speaker} logic/seats)]
    (concat [[(agent speaker) false]]
            (for [x others polarity [true false]] [(agent x) polarity])
            (when (= 2 level)
              (for [[i x] (map-indexed vector others) y (drop (inc i) others)]
                [[:or (agent x) (agent y)] true])))))

(defn world [agent-seat]
  (into {} (for [seat logic/seats] [seat (if (= seat agent-seat) :agent :human)])))

(defn valid?
  "Puzzle rules: the true world fits the opening and it is the only one that does."
  [{:keys [true-world opening]}]
  (= [true-world] (logic/consistent opening)))

(defn- uses-or? [{:keys [opening]}]
  (some (fn [[_ [op]]] (= :or op)) opening))

(defn level
  "Difficulty for train number n (1-based): plain lines first, then 'or' lines."
  [n]
  (if (<= n 3) 1 2))

(defn generate
  "[puzzle, next seed] for difficulty level (1 or 2)."
  [s level]
  (loop [s s]
    (let [[agent-index s] (draw s 4)
          true-world (world (logic/seats agent-index))
          [opening s] (reduce (fn [[opening s] speaker]
                                (let [sayable (filter (fn [[prop polarity]]
                                                        (= polarity (logic/can-say? true-world speaker prop)))
                                                      (lines-for speaker level))
                                      [i s] (draw s (count sayable))
                                      [prop polarity] (nth sayable i)]
                                  [(conj opening [speaker prop polarity]) s]))
                              [[] s]
                              logic/seats)
          puzzle {:true-world true-world :opening opening}]
      (if (and (valid? puzzle) (= (= 2 level) (boolean (uses-or? puzzle))))
        [puzzle s]
        (recur s)))))

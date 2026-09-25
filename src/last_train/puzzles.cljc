(ns last-train.puzzles
  "Seeded puzzle generator. A puzzle is four passengers and one line each;
  the lines alone must leave exactly one possible Agent. Seeds thread through
  a Park-Miller generator so a run replays identically in Clojure and
  ClojureScript."
  (:require [last-train.logic :as logic]))

(def personas
  [{:name "Vera" :bio "night-shift nurse"}
   {:name "Tomasz" :bio "bike courier"}
   {:name "Ilse" :bio "student with headphones"}
   {:name "Mr. Grey" :bio "grey suit, newspaper"}
   {:name "Priya" :bio "accountant hugging a briefcase"}
   {:name "Dmitri" :bio "sharp suit, earpiece"}
   {:name "Rosa" :bio "busker with a battered guitar"}
   {:name "Old Sam" :bio "retired conductor"}
   {:name "Nadia" :bio "ER doctor"}
   {:name "Kofi" :bio "security guard with a paperback"}
   {:name "Harlan" :bio "sunglasses at 3 AM"}
   {:name "Mei" :bio "baker on the early shift"}
   {:name "Jonas" :bio "janitor with a ring of keys"}
   {:name "Lena" :bio "hacker with a cracked laptop"}
   {:name "Big Ray" :bio "bouncer off duty"}
   {:name "Mr. White" :bio "pressed shirt, perfectly still"}
   {:name "Anika" :bio "flight attendant in uniform"}
   {:name "Victor" :bio "salesman with a frozen smile"}
   {:name "Paolo" :bio "chef smelling of garlic"}
   {:name "June" :bio "teenager drawing white rabbits"}])

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

(defn- pick-distinct
  "[k distinct items of coll, the next seed]."
  [s k coll]
  (loop [s s left (vec coll) picked []]
    (if (= k (count picked))
      [picked s]
      (let [[i s] (draw s (count left))]
        (recur s (into (subvec left 0 i) (subvec left (inc i))) (conj picked (left i)))))))

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
          [cast s] (pick-distinct s 4 personas)
          [opening s] (reduce (fn [[opening s] speaker]
                                (let [sayable (filter (fn [[prop polarity]]
                                                        (= polarity (logic/can-say? true-world speaker prop)))
                                                      (lines-for speaker level))
                                      [i s] (draw s (count sayable))
                                      [prop polarity] (nth sayable i)]
                                  [(conj opening [speaker prop polarity]) s]))
                              [[] s]
                              logic/seats)
          puzzle {:true-world true-world
                  :personas (zipmap logic/seats cast)
                  :opening opening}]
      (if (and (valid? puzzle) (= (= 2 level) (boolean (uses-or? puzzle))))
        [puzzle s]
        (recur s)))))

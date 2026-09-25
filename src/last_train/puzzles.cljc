(ns last-train.puzzles
  "Curated puzzles, the puzzle rules they must follow, and picking one."
  (:require [last-train.logic :as logic]))

(defn- agent [seat] [:is seat :agent])
(defn- agent-either [x y] [:or (agent x) (agent y)])

(defn- world [agent-seat]
  (into {} (for [seat logic/seats] [seat (if (= seat agent-seat) :agent :human)])))

(def reference
  "The reference puzzle: two passengers point at each other."
  {:name "reference"
   :true-world (world :A)
   :personas {:A {:name "Vera" :bio "night-shift nurse"}
              :B {:name "Tomasz" :bio "bike courier"}
              :C {:name "Ilse" :bio "student with headphones"}
              :D {:name "Mr. Grey" :bio "man in a grey suit with a newspaper"}}
   :opening [[:A (agent :B) true]
             [:B (agent :A) true]
             [:C (agent :C) false]
             [:D (agent :C) false]]})

(def commuters
  {:name "commuters"
   :true-world (world :B)
   :personas {:A {:name "Priya" :bio "accountant hugging a briefcase"}
              :B {:name "Dmitri" :bio "sharp suit, earpiece, no expression"}
              :C {:name "Rosa" :bio "busker with a battered guitar"}
              :D {:name "Old Sam" :bio "retired conductor who still checks tickets"}}
   :opening [[:A (agent-either :B :C) true]
             [:B (agent :A) true]
             [:C (agent :D) false]
             [:D (agent :D) false]]})

(def night-shift
  {:name "night-shift"
   :true-world (world :C)
   :personas {:A {:name "Nadia" :bio "ER doctor with blood on her sneakers"}
              :B {:name "Kofi" :bio "security guard reading a paperback"}
              :C {:name "Harlan" :bio "wears sunglasses at 3 AM"}
              :D {:name "Mei" :bio "baker heading to the early shift"}}
   :opening [[:A (agent-either :C :D) true]
             [:B (agent :A) false]
             [:C (agent :D) true]
             [:D (agent :D) false]]})

(def terminus
  {:name "terminus"
   :true-world (world :D)
   :personas {:A {:name "Jonas" :bio "janitor with a ring of keys"}
              :B {:name "Lena" :bio "hacker with a cracked laptop"}
              :C {:name "Big Ray" :bio "bouncer off duty"}
              :D {:name "Mr. White" :bio "pressed shirt, perfectly still"}}
   :opening [[:A (agent :C) false]
             [:B (agent-either :A :D) true]
             [:C (agent :C) false]
             [:D (agent :B) true]]})

(def red-eye
  {:name "red-eye"
   :true-world (world :B)
   :personas {:A {:name "Anika" :bio "flight attendant still in uniform"}
              :B {:name "Victor" :bio "salesman whose smile never moves"}
              :C {:name "Paolo" :bio "chef smelling of garlic"}
              :D {:name "June" :bio "teenager drawing white rabbits"}}
   :opening [[:A (agent-either :B :C) true]
             [:B (agent :C) true]
             [:C (agent :C) false]
             [:D (agent :A) false]]})

(def catalog [reference commuters night-shift terminus red-eye])

(defn by-name [name]
  (first (filter #(= name (:name %)) catalog)))

(defn valid?
  "Puzzle rules: the true world fits the opening, the opening leaves 2 or 3
  Agent suspects (so it is not solved at boarding), and two questions always
  find the Agent (the player gets three)."
  [{:keys [true-world opening]}]
  (let [worlds (logic/consistent opening)]
    (boolean
      (and (some #{true-world} worlds)
           (<= 2 (count (logic/agent-seats worlds)) 3)
           (logic/solvable? worlds 2)))))

(defn pick
  "A random puzzle other than the one named previous; rand-int is (fn [n]) -> 0..n-1."
  [rand-int previous]
  (let [choices (or (seq (remove #(= previous (:name %)) catalog)) catalog)]
    (nth choices (rand-int (count choices)))))

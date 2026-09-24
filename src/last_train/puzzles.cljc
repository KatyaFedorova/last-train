(ns last-train.puzzles
  "Curated puzzles, the puzzle rules they must follow, and picking one."
  (:require [last-train.logic :as logic]))

(def reference
  "The verified reference puzzle (spec section 2, part 8)."
  {:name "reference"
   :true-world {:A :agent :B :sleeper :C :sleeper :D :awake}
   :personas {:A {:name "Vera" :bio "night-shift nurse"}
              :B {:name "Tomasz" :bio "bike courier"}
              :C {:name "Ilse" :bio "student with headphones"}
              :D {:name "Mr. Grey" :bio "man in a grey suit with a newspaper"}}
   :opening [[:A [:count-eq :agent 0] true]
             [:B [:not [:is :A :agent]] true]
             [:C [:same :B :C] true]
             [:D [:same :B :C] true]]})

(def commuters
  {:name "commuters"
   :true-world {:A :sleeper :B :agent :C :awake :D :sleeper}
   :personas {:A {:name "Priya" :bio "accountant hugging a briefcase"}
              :B {:name "Dmitri" :bio "sharp suit, earpiece, no expression"}
              :C {:name "Rosa" :bio "busker with a battered guitar"}
              :D {:name "Old Sam" :bio "retired conductor who still checks tickets"}}
   :opening [[:A [:not [:is :B :agent]] true]
             [:B [:same :C :D] true]
             [:C [:same :A :D] true]
             [:D [:same :A :B] true]]})

(def night-shift
  {:name "night-shift"
   :true-world {:A :awake :B :sleeper :C :agent :D :sleeper}
   :personas {:A {:name "Nadia" :bio "ER doctor with blood on her sneakers"}
              :B {:name "Kofi" :bio "security guard reading a paperback"}
              :C {:name "Harlan" :bio "wears sunglasses at 3 AM"}
              :D {:name "Mei" :bio "baker heading to the early shift"}}
   :opening [[:A [:not [:is :A :agent]] true]
             [:B [:same :C :D] true]
             [:C [:not [:same :B :D]] true]
             [:D [:same :B :C] true]]})

(def terminus
  {:name "terminus"
   :true-world {:A :sleeper :B :awake :C :sleeper :D :agent}
   :personas {:A {:name "Jonas" :bio "janitor with a ring of keys"}
              :B {:name "Lena" :bio "hacker with a cracked laptop"}
              :C {:name "Big Ray" :bio "bouncer off duty"}
              :D {:name "Mr. White" :bio "pressed shirt, perfectly still"}}
   :opening [[:A [:not [:is :B :awake]] true]
             [:B [:same :A :C] true]
             [:C [:same :A :D] true]
             [:D [:same :B :C] true]]})

(def red-eye
  {:name "red-eye"
   :true-world {:A :sleeper :B :agent :C :sleeper :D :awake}
   :personas {:A {:name "Anika" :bio "flight attendant still in uniform"}
              :B {:name "Victor" :bio "salesman whose smile never moves"}
              :C {:name "Paolo" :bio "chef smelling of garlic"}
              :D {:name "June" :bio "teenager drawing white rabbits"}}
   :opening [[:A [:not [:is :B :awake]] true]
             [:B [:not [:same :A :C]] true]
             [:C [:same :A :B] true]
             [:D [:same :A :C] true]]})

(def catalog [reference commuters night-shift terminus red-eye])

(defn by-name [name]
  (first (filter #(= name (:name %)) catalog)))

(defn valid?
  "Puzzle rules (game spec section 2, part 5): the true world fits the opening,
  2-4 worlds remain, one question cannot solve it and two adaptive questions
  always can. Not solvable in one also rules out a single Agent seat, since
  solvable? is true once at most one Agent seat is left."
  [{:keys [true-world opening]}]
  (let [worlds (logic/consistent opening)]
    (boolean
      (and (some #{true-world} worlds)
           (<= 2 (count worlds) 4)
           (not (logic/solvable? worlds 1))
           (logic/solvable? worlds 2)))))

(defn pick
  "A random puzzle other than the one named previous; rand-int is (fn [n]) -> 0..n-1."
  [rand-int previous]
  (let [choices (or (seq (remove #(= previous (:name %)) catalog)) catalog)]
    (nth choices (rand-int (count choices)))))

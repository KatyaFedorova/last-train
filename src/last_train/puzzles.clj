(ns last-train.puzzles
  "Curated puzzles.")

(def reference
  "The verified reference puzzle (spec section 2, part 8)."
  {:true-world {:A :agent :B :sleeper :C :sleeper :D :awake}
   :personas {:A {:name "Vera" :bio "night-shift nurse"}
              :B {:name "Tomasz" :bio "bike courier"}
              :C {:name "Ilse" :bio "student with headphones"}
              :D {:name "Mr. Grey" :bio "man in a grey suit with a newspaper"}}
   :opening [[:A [:count-eq :agent 0] true]
             [:B [:not [:is :A :agent]] true]
             [:C [:same :B :C] true]
             [:D [:same :B :C] true]]})

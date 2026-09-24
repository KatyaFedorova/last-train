(ns last-train.logic
  "Deterministic logic engine: worlds, propositions and who can say what.")

(def seats [:A :B :C :D])
(def roles [:agent :awake :sleeper])

(def all-worlds
  (vec (for [agent seats
             awake seats
             :when (not= agent awake)]
         (into {} (for [seat seats]
                    [seat (condp = seat agent :agent awake :awake :sleeper)])))))

(def all-sleepers (zipmap seats (repeat :sleeper)))

(defn agent-seat [world]
  (first (filter #(= :agent (world %)) seats)))

(defn agent-seats [worlds]
  (let [found (set (map agent-seat worlds))]
    (filterv found seats)))

(defn evaluate [prop world]
  (let [[op x y] prop]
    (case op
      :is (= y (world x))
      :same (= (world x) (world y))
      :count-eq (= y (count (filter #{x} (vals world))))
      :not (not (evaluate x world))
      :and (and (evaluate x world) (evaluate y world))
      :or (or (evaluate x world) (evaluate y world)))))

(defn can-say? [world speaker prop]
  (case (world speaker)
    :awake (evaluate prop world)
    :agent (not (evaluate prop world))
    :sleeper (evaluate prop all-sleepers)))

(defn consistent
  "Worlds in which every fact [speaker prop asserted?] holds."
  ([facts] (consistent facts all-worlds))
  ([facts worlds]
   (filterv (fn [world]
              (every? (fn [[speaker prop asserted]]
                        (= asserted (can-say? world speaker prop)))
                      facts))
            worlds)))

(def seat-pairs
  (for [[i x] (map-indexed vector seats)
        y (drop (inc i) seats)]
    [x y]))

(def questions
  (vec (concat (for [speaker seats seat seats role roles]
                 [speaker [:is seat role]])
               (for [speaker seats [x y] seat-pairs]
                 [speaker [:same x y]]))))

(defn solvable? [worlds depth]
  (cond
    (<= (count (agent-seats worlds)) 1) true
    (zero? depth) false
    :else (boolean
            (some (fn [[speaker prop]]
                    (let [{yes true no false} (group-by #(can-say? % speaker prop) worlds)]
                      (and yes no
                           (solvable? yes (dec depth))
                           (solvable? no (dec depth)))))
                  questions))))

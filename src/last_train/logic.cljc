(ns last-train.logic
  "Deterministic logic engine: worlds, propositions and who can say what.
  One Agent always lies; the other three passengers are humans who always
  tell the truth.")

(def seats [:A :B :C :D])
(def roles [:agent :human])

(def all-worlds
  (vec (for [agent seats]
         (into {} (for [seat seats]
                    [seat (if (= seat agent) :agent :human)])))))

(defn agent-seat [world]
  (first (filter #(= :agent (world %)) seats)))

(defn agent-seats [worlds]
  (let [found (set (map agent-seat worlds))]
    (filterv found seats)))

(defn evaluate [prop world]
  (let [[op x y] prop]
    (case op
      :is (= y (world x))
      :not (not (evaluate x world))
      :and (and (evaluate x world) (evaluate y world))
      :or (or (evaluate x world) (evaluate y world)))))

(defn can-say? [world speaker prop]
  (case (world speaker)
    :human (evaluate prop world)
    :agent (not (evaluate prop world))))

(defn consistent
  "Worlds in which every fact [speaker prop asserted?] holds."
  ([facts] (consistent facts all-worlds))
  ([facts worlds]
   (filterv (fn [world]
              (every? (fn [[speaker prop asserted]]
                        (= asserted (can-say? world speaker prop)))
                      facts))
            worlds)))

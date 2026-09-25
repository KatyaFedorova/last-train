(ns last-train.puzzle-search
  "Dev tool: bb puzzle-search <agent-seat> [limit]
  Prints opening statement sets that follow the puzzle rules for that true world.
  Kept out of src/ so test tools skip it."
  (:require [last-train.english :as english]
            [last-train.logic :as logic]
            [last-train.puzzles :as puzzles]))

(def ^:private props
  (concat (for [seat logic/seats
                prop [[:is seat :agent] [:not [:is seat :agent]]]]
            prop)
          (for [[i x] (map-indexed vector logic/seats)
                y (drop (inc i) logic/seats)]
            [:or [:is x :agent] [:is y :agent]])))

(defn- sayable [world seat]
  (for [prop props :when (logic/can-say? world seat prop)]
    [seat prop true]))

(defn candidates
  "Lazy seq of valid openings for the true world."
  [world]
  (for [a (sayable world :A) b (sayable world :B)
        c (sayable world :C) d (sayable world :D)
        :let [opening [a b c d]]
        :when (puzzles/valid? {:true-world world :opening opening})]
    opening))

(defn -main [agent & [limit]]
  (let [world (first (filter #(= (keyword agent) (logic/agent-seat %)) logic/all-worlds))]
    (doseq [opening (take (or (some-> limit parse-long) 10) (candidates world))]
      (prn opening)
      (doseq [[seat prop polarity] opening]
        (println " " (name seat) (english/render-statement prop polarity {:self seat})))
      (println "  suspects:" (logic/agent-seats (logic/consistent opening))))))

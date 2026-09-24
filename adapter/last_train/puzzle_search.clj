(ns last-train.puzzle-search
  "Dev tool: bb puzzle-search <agent-seat> <awake-seat> [limit]
  Prints opening statement sets that follow the puzzle rules for that true world.
  Kept out of src/ so test tools skip it."
  (:require [last-train.english :as english]
            [last-train.logic :as logic]
            [last-train.puzzles :as puzzles]))

(def ^:private props
  (concat (for [seat logic/seats role logic/roles
                prop [[:is seat role] [:not [:is seat role]]]]
            prop)
          (for [[x y] logic/seat-pairs
                prop [[:same x y] [:not [:same x y]]]]
            prop)
          [[:count-eq :agent 0]]))

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

(defn -main [agent awake & [limit]]
  (let [agent (keyword agent)
        awake (keyword awake)
        world (into {} (for [seat logic/seats]
                         [seat (condp = seat agent :agent awake :awake :sleeper)]))]
    (doseq [opening (take (or (some-> limit parse-long) 10) (candidates world))]
      (prn opening)
      (doseq [[seat prop polarity] opening]
        (println " " (name seat) (english/render-statement prop polarity {:self seat})))
      (println "  worlds:" (count (logic/consistent opening))
               "agents:" (logic/agent-seats (logic/consistent opening))))))

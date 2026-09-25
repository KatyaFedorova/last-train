(ns last-train.game-property-spec
  (:require [speclj.core :refer :all]
            [last-train.check :refer [passes?]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [last-train.game :as game]
            [last-train.generators :as g]
            [last-train.puzzles :as puzzles]))

(defn- states [lines]
  (reductions (fn [state line] (:state (game/handle state line)))
              (:state (game/start puzzles/reference))
              lines))

(describe "Game properties"
  (it "keeps its invariants under any player input"
    (should (passes? (prop/for-all [lines (gen/vector g/player-line 0 8)]
                       (let [ss (states lines)
                             true-world (:true-world puzzles/reference)]
                         (and (every? #(<= 0 (:questions-left %) 3) ss)
                              (every? #(some #{true-world} (:live-worlds %)) ss)
                              (apply >= (map :questions-left ss))))))))

  (it "never changes once the game is over"
    (should (passes? (prop/for-all [lines (gen/vector g/player-line 0 8) after g/player-line]
                       (let [over (last (states (conj lines "accuse A")))]
                         (= over (:state (game/handle over after)))))))))

(ns last-train.game-property-spec
  (:require [speclj.core :refer :all]
            [last-train.check :refer [passes?]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [last-train.game :as game]
            [last-train.generators :as g]
            [last-train.logic :as logic]))

(defn- states [seed lines]
  (reductions (fn [state line] (:state (game/handle state line)))
              (:state (game/start seed))
              lines))

(describe "Game properties"
  (it "keeps its invariants under any player input"
    (should (passes? (prop/for-all [seed gen/nat lines (gen/vector g/player-line 0 14)]
                       (let [ss (states seed lines)]
                         (and (every? #(<= 1 (:train %) game/trains) ss)
                              (every? #(<= 0 (:right %) (:train %)) ss)
                              (every? #(= [(get-in % [:puzzle :true-world])]
                                          (logic/consistent (get-in % [:puzzle :opening])))
                                      ss)
                              (apply <= (map :train ss))
                              (apply <= (map :score ss))))))))

  (it "never changes once the run is over"
    (should (passes? (prop/for-all [seed gen/nat after g/player-line]
                       (let [over (last (states seed (repeat game/trains "time")))]
                         (and (:over? over) (= over (:state (game/handle over after))))))))))

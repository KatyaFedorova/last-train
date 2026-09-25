(ns last-train.terminal-property-spec
  (:require [speclj.core :refer :all]
            [last-train.check :refer [passes?]]
            [clojure.string :as str]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [last-train.game :as game]
            [last-train.generators :as g]
            [last-train.puzzles :as puzzles]
            [last-train.terminal :as terminal]))

(defn- first-draw [_] 0)

(def ^:private web-only #{"new" "restart" "play again" "help"})

(def ^:private game-line
  (gen/such-that #(not (web-only (str/lower-case (str/trim %)))) g/player-line))

(defn- cli-transcript [puzzle lines]
  (reduce (fn [{:keys [state output]} line]
            (let [step (game/handle state line)]
              {:state (:state step) :output (into output (:output step))}))
          (update (game/start puzzle) :output vec)
          lines))

(defn- web-session [puzzle lines]
  (reduce #(terminal/submit %1 %2 first-draw)
          (terminal/boot {:puzzle-param (:name puzzle) :rand-int first-draw})
          lines))

(describe "Web terminal properties"
  (it "shows exactly what the terminal game prints, plus echoes and the new-game hint"
    (should (passes? (prop/for-all [puzzle (gen/elements puzzles/catalog)
                                    lines (gen/vector game-line 0 8)]
                       (let [web (->> (:lines (web-session puzzle lines))
                                      (remove #(= :player (:kind %)))
                                      (map :text)
                                      (remove #{"Type \"new\" to play again."}))]
                         (= (:output (cli-transcript puzzle lines)) web))))))

  (it "never reveals a role before the game is over"
    (should (passes? (prop/for-all [puzzle (gen/elements puzzles/catalog)
                                    lines (gen/vector g/player-line 0 8)]
                       (let [session (web-session puzzle lines)]
                         (or (get-in session [:game :over?])
                             (not-any? #(re-find #":agent|:human|true-world" (:text %))
                                       (:lines session)))))))))

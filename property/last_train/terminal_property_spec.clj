(ns last-train.terminal-property-spec
  (:require [speclj.core :refer :all]
            [last-train.check :refer [passes?]]
            [clojure.string :as str]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [last-train.game :as game]
            [last-train.generators :as g]
            [last-train.terminal :as terminal]))

(defn- first-draw [_] 0)

(def ^:private web-only #{"new" "restart" "play again" "help"})

(def ^:private game-line
  (gen/such-that #(not (web-only (str/lower-case (str/trim %)))) g/player-line))

(defn- cli-transcript [seed lines]
  (reduce (fn [{:keys [state output]} line]
            (let [step (game/handle state line)]
              {:state (:state step) :output (into output (:output step))}))
          (update (game/start seed) :output vec)
          lines))

(defn- web-session [seed lines]
  (reduce #(terminal/submit %1 %2 first-draw)
          (terminal/boot {:seed-param (str seed) :rand-int first-draw})
          lines))

(describe "Web terminal properties"
  (it "shows exactly what the terminal game prints, plus echoes and the new-game hint"
    (should (passes? (prop/for-all [seed gen/nat
                                    lines (gen/vector game-line 0 8)]
                       (let [web (->> (:lines (web-session seed lines))
                                      (remove #(= :player (:kind %)))
                                      (map :text)
                                      (remove #{"Press Enter or type \"new\" to play again."}))]
                         (= (:output (cli-transcript seed lines)) web))))))

  (it "never prints a role"
    (should (passes? (prop/for-all [seed gen/nat
                                    lines (gen/vector g/player-line 0 8)]
                       (let [session (web-session seed lines)]
                         (not-any? #(re-find #":agent|:human|true-world" (:text %))
                                   (:lines session))))))))

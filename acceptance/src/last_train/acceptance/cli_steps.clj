(ns last-train.acceptance.cli-steps
  "Step handlers that play the game through its command line interface."
  (:require [clojure.string :as str]
            [last-train.acceptance.runtime :refer [check]]
            [last-train.acceptance.transcript :as transcript]
            [last-train.cli :as cli]
            [last-train.game :as game]))

(defn- start [world command]
  (let [[program & args] (str/split (str/trim command) #"\s+")
        {:keys [error seed]} (cli/parse-args args)]
    (check (= "last-train" program) (str "Unknown program: " program))
    (check (nil? error) (str "CLI refused to start: " error))
    (let [{:keys [state output]} (game/start seed)]
      (assoc world :session {:state state :output (vec output) :last (vec output)}))))

(defn game-state [world] (get-in world [:session :state]))

(defn type-line [world input opts]
  (check (:session world) "No game is running")
  (let [{:keys [state output]} (game/handle (game-state world) input opts)]
    (update world :session #(-> %
                                (assoc :state state :last (vec output))
                                (update :output into output)))))

(defn output [world] (get-in world [:session :output]))
(defn last-output [world] (get-in world [:session :last]))

(defn- check-statement-count [world n]
  (check (= (parse-long n) (count (transcript/statements (last-output world))))
         (str "Displayed statements: " (vec (transcript/statements (last-output world)))))
  world)

(defn- check-one-agent [world]
  (transcript/check-one-agent (last-output world))
  world)

(defn- check-no-roles [world]
  (transcript/check-no-roles (output world))
  world)

(def handlers
  [[#"^I start \"(.+)\"$" start]
   [#"^the game displays (\d+) passenger statements$" check-statement-count]
   [#"^the statements leave exactly one possible Agent$" check-one-agent]
   [#"^the game does not display the true passenger roles$" check-no-roles]
   [#"^I type \"(.+)\"$" (fn [world input] (type-line world input {}))]])

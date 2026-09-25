(ns last-train.acceptance.steps
  "All project step handlers."
  (:require [clojure.string :as str]
            [last-train.acceptance.cli-steps :as cli-steps]
            [last-train.acceptance.engine-steps :as engine-steps]
            [last-train.acceptance.runtime :refer [check]]
            [last-train.acceptance.transcript :as transcript]
            [last-train.acceptance.web-steps :as web-steps]
            [last-train.game :as game]
            [last-train.logic :as logic]))

(defn- web? [world] (boolean (:web world)))

(defn- game-state [world]
  (if (web? world) (web-steps/game-state world) (cli-steps/game-state world)))

(defn- recent-lines [world]
  (if (web? world) (web-steps/new-lines world) (cli-steps/last-output world)))

(defn- send-input
  "Type input into the open game, remembering the train it answered."
  ([world input] (send-input world input {}))
  ([world input opts]
   (let [world (assoc world :answered (game-state world))]
     (if (web? world)
       (web-steps/type-command world input opts)
       (cli-steps/type-line world input opts)))))

(defn- agent-letter [world] (name (game/agent-seat (game-state world))))

(defn- name-agent [world] (send-input world (agent-letter world)))

(defn- name-agent-with [world seconds]
  (send-input world (agent-letter world) {:seconds-left (parse-long seconds)}))

(defn- name-wrong [world]
  (let [agent (game/agent-seat (game-state world))]
    (send-input world (name (first (remove #{agent} logic/seats))))))

(defn- name-agent-every-train [world]
  (reduce (fn [world _] (name-agent world)) world (range game/trains)))

(defn- check-see [world text]
  (transcript/check-see (recent-lines world) text)
  world)

(defn- check-lie [world]
  (let [answered (:answered world)
        agent (game/agent-seat answered)]
    (transcript/check-see (recent-lines world)
                          (str (get-in answered [:puzzle :personas agent :name])
                               "'s line was the lie: \"" ((game/lines answered) agent) "\""))
    world))

(defn- check-train [world n]
  (check (= (parse-long n) (:train (game-state world))) (str "On train " (:train (game-state world))))
  world)

(defn- check-score [world score]
  (transcript/check-score (recent-lines world) score)
  world)

(defn- check-over [world]
  (check (:over? (game-state world)) "The run is still going")
  (transcript/check-game-over-shown (recent-lines world))
  world)

(defn- check-tip [world]
  (let [[_ who] (some #(re-matches #"^Operator: \"Tip: (.+) is telling the truth\.\"$" %) (recent-lines world))
        state (game-state world)
        agent (game/agent-seat state)]
    (check who (str "No tip in " (vec (recent-lines world))))
    (check (not= who (get-in state [:puzzle :personas agent :name])) "The tip names the Agent")
    world))

(def handlers
  (concat [[#"^I name the Agent$" name-agent]
           [#"^I name the Agent with (\d+) seconds left$" name-agent-with]
           [#"^I name the Agent on every train$" name-agent-every-train]
           [#"^I name a passenger who is not the Agent$" name-wrong]
           [#"^the clock runs out$" #(send-input % "time")]
           [#"^I see \"(.+)\"$" check-see]
           [#"^I see the Agent's lie$" check-lie]
           [#"^I am still on train (\d+)$" check-train]
           [#"^the score is (\d+)$" check-score]
           [#"^the game is over$" check-over]
           [#"^the tip names a passenger who is not the Agent$" check-tip]]
          engine-steps/handlers
          cli-steps/handlers
          web-steps/handlers))

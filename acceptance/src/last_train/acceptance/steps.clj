(ns last-train.acceptance.steps
  "All project step handlers."
  (:require [last-train.acceptance.cli-steps :as cli-steps]
            [last-train.acceptance.engine-steps :as engine-steps]
            [last-train.acceptance.transcript :as transcript]
            [last-train.acceptance.web-steps :as web-steps]))

(defn- ask
  "Ask through the running game when there is one, else ask the engine."
  [world speaker text]
  (if (:session world)
    (cli-steps/ask-cli world speaker text)
    (engine-steps/ask-engine world speaker text)))

(defn- live-worlds [world]
  (cond
    (:web world) (web-steps/live-worlds world)
    (:session world) (get-in world [:session :state :live-worlds])
    :else (:worlds world)))

(defn- check-agent-seats [world expected]
  (engine-steps/check-agent-seats (live-worlds world) expected)
  world)

(defn- on-game
  "Handler that uses the web page when one is open, else the terminal."
  [cli-handler web-handler]
  (fn [world & args]
    (apply (if (:web world) web-handler cli-handler) world args)))

(def ^:private recent-lines
  "Lines the last move added."
  (on-game cli-steps/last-output web-steps/new-lines))


(defn- check-see [world text]
  (transcript/check-see (recent-lines world) text)
  world)

(defn- check-score [world score]
  (transcript/check-score (recent-lines world) score)
  world)

(def handlers
  (concat [[#"^I ask passenger (\S+) whether (.+)$" ask]
           [#"^the (?:only )?possible Agent seats? (?:are|is) (.+)$" check-agent-seats]
           [#"^I see \"(.+)\"$" check-see]
           [#"^I accuse passenger (\S+)$"
            (on-game cli-steps/accuse web-steps/accuse)]
           [#"^the score is (\d+)$" check-score]
           [#"^the game is over$" (on-game cli-steps/check-over web-steps/check-over)]]
          engine-steps/handlers
          cli-steps/handlers
          web-steps/handlers))

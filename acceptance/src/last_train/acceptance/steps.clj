(ns last-train.acceptance.steps
  "All project step handlers."
  (:require [last-train.acceptance.cli-steps :as cli-steps]
            [last-train.acceptance.engine-steps :as engine-steps]
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
  "Step handler that plays through the web page when one is open, else the terminal."
  [cli-handler web-handler]
  (fn [world & args]
    (apply (if (:web world) web-handler cli-handler) world args)))

(def handlers
  (concat [[#"^I ask passenger (\S+) whether (?!\S+ and then \S+ are Agents$)(.+)$" ask]
           [#"^the (?:only )?possible Agent seats? (?:are|is) (.+)$" check-agent-seats]
           [#"^I see \"(.+)\"$" (on-game cli-steps/check-see web-steps/check-see)]
           [#"^I remain in (.+)$" (on-game cli-steps/check-round web-steps/check-round)]
           [#"^the game advances to the (.+) round$"
            (on-game cli-steps/check-advanced-round web-steps/check-advanced-round)]
           [#"^I accuse passenger (\S+)(?: and name passenger (\S+) as the Awake ally)?$"
            (on-game cli-steps/accuse web-steps/accuse)]
           [#"^the score is (\d+)$" (on-game cli-steps/check-score web-steps/check-score)]
           [#"^the game is over$" (on-game cli-steps/check-over web-steps/check-over)]]
          engine-steps/handlers
          cli-steps/handlers
          web-steps/handlers))

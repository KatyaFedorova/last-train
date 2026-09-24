(ns last-train.acceptance.steps
  "All project step handlers."
  (:require [last-train.acceptance.cli-steps :as cli-steps]
            [last-train.acceptance.engine-steps :as engine-steps]))

(defn- ask
  "Ask through the running game when there is one, else ask the engine."
  [world speaker text]
  (if (:session world)
    (cli-steps/ask-cli world speaker text)
    (engine-steps/ask-engine world speaker text)))

(defn- check-agent-seats [world expected]
  (engine-steps/check-agent-seats (or (get-in world [:session :state :live-worlds]) (:worlds world)) expected)
  world)

(def handlers
  (concat [[#"^I ask passenger (\S+) whether (.+)$" ask]
           [#"^the (?:only )?possible Agent seats? (?:are|is) (.+)$" check-agent-seats]]
          engine-steps/handlers
          cli-steps/handlers))

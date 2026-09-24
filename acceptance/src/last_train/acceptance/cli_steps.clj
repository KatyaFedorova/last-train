(ns last-train.acceptance.cli-steps
  "Step handlers that play the game through its command line interface."
  (:require [clojure.string :as str]
            [last-train.acceptance.engine-steps :as engine]
            [last-train.acceptance.runtime :refer [check]]
            [last-train.acceptance.transcript :as transcript]
            [last-train.cli :as cli]
            [last-train.english :as english]
            [last-train.game :as game]))

(defn- start [world command]
  (let [[program & args] (str/split (str/trim command) #"\s+")
        {:keys [error puzzle]} (cli/parse-args args)]
    (check (= "last-train" program) (str "Unknown program: " program))
    (check (nil? error) (str "CLI refused to start: " error))
    (let [{:keys [state output]} (game/start puzzle)]
      (assoc world :session {:state state :output (vec output) :last (vec output) :counts []}))))

(defn type-line [world input]
  (check (:session world) "No game is running")
  (let [{:keys [state output]} (game/handle (get-in world [:session :state]) input)]
    (-> world
        (update :session assoc :state state :last (vec output))
        (update-in [:session :counts] conj (transcript/questions-left (get-in world [:session :output])))
        (update-in [:session :output] into output))))

(defn ask-cli [world speaker text]
  (type-line world (str "ask " speaker " " (english/render-question (engine/prop text) {}))))

(defn output [world] (get-in world [:session :output]))
(defn last-output [world] (get-in world [:session :last]))

(defn- check-statement-count [world n]
  (check (= (parse-long n) (count (transcript/statements (last-output world))))
         (str "Displayed statements: " (vec (transcript/statements (last-output world)))))
  world)

(defn- check-reference-statements [world]
  (transcript/check-reference-statements (last-output world))
  world)

(defn- check-questions-left [world n]
  (transcript/check-questions-left (output world) n)
  world)

(defn- check-questions-unchanged [world n]
  (check (= (peek (get-in world [:session :counts])) (transcript/questions-left (output world)))
         "Question count changed")
  (check-questions-left world n))

(defn- check-questions-decreased [world n]
  (check (> (peek (get-in world [:session :counts])) (transcript/questions-left (output world)))
         "Question count did not drop")
  (check-questions-left world n))

(defn- check-no-roles [world]
  (transcript/check-no-roles (output world))
  world)

(defn- check-template-answer [world seat expected]
  (transcript/check-answer (last-output world) seat expected)
  world)

(defn- ask-twice [world speaker first-seat second-seat]
  (-> world
      (ask-cli speaker (str first-seat " is an Agent"))
      (ask-cli speaker (str second-seat " is an Agent"))))

(defn- check-both-answers [world seat expected]
  (let [answers (transcript/answers-of (output world) seat)]
    (check (= [(engine/yes-no expected) (engine/yes-no expected)] (take-last 2 answers))
           (str "Answers were " (vec answers)))
    world))

(defn accuse [world agent ally]
  (type-line world (str "accuse " agent (when ally (str " ally " ally)))))

(defn check-over [world]
  (check (get-in world [:session :state :over?]) "The game is still running")
  (transcript/check-game-over-shown (last-output world))
  world)

(def handlers
  [[#"^I start \"(.+)\"$" start]
   [#"^the game displays (\d+) passenger statements$" check-statement-count]
   [#"^the statements convey the reference opening statements$" check-reference-statements]
   [#"^the game has (\d+) questions available$" check-questions-left]
   [#"^the question count remains (\d+)$" check-questions-unchanged]
   [#"^the question count decreases to (\d+)$" check-questions-decreased]
   [#"^I have (\d+) unused questions?$" check-questions-left]
   [#"^the game does not display the true passenger roles$" check-no-roles]
   [#"^I type \"(.+)\"$" type-line]
   [#"^passenger (\S+)'s template answer is (\S+)$" check-template-answer]
   [#"^I ask passenger (\S+) about (\S+) and then (\S+) as Agents$" ask-twice]
   [#"^passenger (\S+) answers (\S+) to both questions$" check-both-answers]])

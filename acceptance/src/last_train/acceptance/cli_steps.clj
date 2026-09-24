(ns last-train.acceptance.cli-steps
  "Step handlers that play the game through its command line interface."
  (:require [clojure.string :as str]
            [last-train.acceptance.engine-steps :as engine]
            [last-train.acceptance.runtime :refer [check]]
            [last-train.cli :as cli]
            [last-train.english :as english]
            [last-train.game :as game]))

(def ^:private statement-line #"^(.+) \(([A-D])\): \"(.*)\"$")

(defn- start [world command]
  (let [[program & args] (str/split (str/trim command) #"\s+")
        {:keys [error puzzle]} (cli/parse-args args)]
    (check (= "last-train" program) (str "Unknown program: " program))
    (check (nil? error) (str "CLI refused to start: " error))
    (let [{:keys [state output]} (game/start puzzle)]
      (assoc world :session {:state state :output (vec output) :last (vec output) :counts []}))))

(defn- questions-left [lines]
  (some->> lines
           (keep #(second (re-matches #"^Questions left: (\d+)$" %)))
           last
           parse-long))

(defn type-line [world input]
  (check (:session world) "No game is running")
  (let [{:keys [state output]} (game/handle (get-in world [:session :state]) input)]
    (-> world
        (update :session assoc :state state :last (vec output))
        (update-in [:session :counts] conj (questions-left (get-in world [:session :output])))
        (update-in [:session :output] into output))))

(defn ask-cli [world speaker text]
  (type-line world (str "ask " speaker " " (english/render-question (engine/prop text) {}))))

(defn- output [world] (get-in world [:session :output]))
(defn- last-output [world] (get-in world [:session :last]))

(defn- statements [lines]
  (keep #(re-matches statement-line %) lines))

(defn- check-statement-count [world n]
  (check (= (parse-long n) (count (statements (last-output world))))
         (str "Displayed statements: " (vec (statements (last-output world)))))
  world)

(defn- check-reference-statements [world]
  (let [shown (statements (last-output world))
        names (into {} (for [[_ persona seat] shown] [(keyword seat) persona]))
        conveyed (set (for [[_ _ seat line] shown]
                        [(keyword seat) (english/parse-prop line {:names names :self (keyword seat)})]))
        expected (set (for [[speaker text polarity] engine/reference-statements]
                        (let [p (engine/prop text)] [(engine/seat speaker) (if polarity p [:not p])])))]
    (check (= expected conveyed) (str "Statements conveyed " conveyed))
    world))

(defn- check-questions-left [world n]
  (check (= (parse-long n) (questions-left (output world)))
         (str "Questions left: " (questions-left (output world))))
  world)

(defn- check-questions-unchanged [world n]
  (check (= (peek (get-in world [:session :counts])) (questions-left (output world))) "Question count changed")
  (check-questions-left world n))

(defn- check-questions-decreased [world n]
  (check (> (peek (get-in world [:session :counts])) (questions-left (output world))) "Question count did not drop")
  (check-questions-left world n))

(defn- check-no-roles [world]
  (let [text (str/join "\n" (output world))]
    (check (not (re-find #"AGENT|AWAKE|SLEEPER" text)) "Role tokens displayed")
    (check (not (re-find #"(?i)is the agent|is your ally|is awake\b" text)) "A role is revealed"))
  world)

(defn- check-see [world text]
  (check (str/includes? (str/join "\n" (last-output world)) text)
         (str "Did not see " (pr-str text) " in " (last-output world)))
  world)

(defn- round-title [lines]
  (some->> lines (keep #(second (re-matches #"^Round \d+ - (.+)$" %))) last))

(defn- check-round [world title]
  (check (nil? (round-title (last-output world))) "The round changed")
  (check (= (str/lower-case title) (str/lower-case (round-title (output world))))
         (str "Round is " (round-title (output world))))
  world)

(defn- check-advanced-round [world title]
  (check (= (str/lower-case title) (some-> (round-title (last-output world)) str/lower-case))
         (str "Advanced to " (round-title (last-output world))))
  world)

(defn- answers-of [lines seat]
  (keep (fn [line]
          (when-let [[_ _ speaker answer] (re-matches #"^(.+) \(([A-D])\): \"(Yes|No)\. .*\"$" line)]
            (when (= seat speaker) (= "Yes" answer))))
        lines))

(defn- check-template-answer [world seat expected]
  (check (= [(engine/yes-no expected)] (answers-of (last-output world) seat))
         (str "Answer lines: " (last-output world)))
  world)

(defn- ask-twice [world speaker first-seat second-seat]
  (-> world
      (ask-cli speaker (str first-seat " is an Agent"))
      (ask-cli speaker (str second-seat " is an Agent"))))

(defn- check-both-answers [world seat expected]
  (let [answers (answers-of (output world) seat)]
    (check (= [(engine/yes-no expected) (engine/yes-no expected)] (take-last 2 answers))
           (str "Answers were " (vec answers)))
    world))

(defn- accuse [world agent ally]
  (type-line world (str "accuse " agent (when ally (str " ally " ally)))))

(defn- check-score [world score]
  (check (some #{(str "Score: " score)} (last-output world)) (str "Output: " (last-output world)))
  world)

(defn- check-over [world]
  (check (get-in world [:session :state :over?]) "The game is still running")
  (check (some #{"GAME OVER"} (last-output world)) "GAME OVER not shown")
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
   [#"^I see \"(.+)\"$" check-see]
   [#"^I remain in (.+)$" check-round]
   [#"^the game advances to the (.+) round$" check-advanced-round]
   [#"^passenger (\S+)'s template answer is (\S+)$" check-template-answer]
   [#"^I ask passenger (\S+) about (\S+) and then (\S+) as Agents$" ask-twice]
   [#"^passenger (\S+) answers (\S+) to both questions$" check-both-answers]
   [#"^I accuse passenger (\S+)(?: and name passenger (\S+) as the Awake ally)?$" accuse]
   [#"^the score is (\d+)$" check-score]
   [#"^the game is over$" check-over]])

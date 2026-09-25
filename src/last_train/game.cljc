(ns last-train.game
  "Pure game loop: questions, accusation and score, with template voices."
  (:require [clojure.string :as str]
            [last-train.english :as english]
            [last-train.logic :as logic]))

(def ^:private noisy "Operator: \"Signal's noisy. Rephrase.\"")

(def syntax "Ask: ask <passenger> Is <passenger> the Agent?   Accuse: accuse <passenger>")

(defn- english-context
  "English context for the puzzle's personas, with self as I/me/you when given."
  ([state] (english-context state nil))
  ([state self] {:names (update-vals (get-in state [:puzzle :personas]) :name) :self self}))

(defn- persona-name [state seat]
  (get-in state [:puzzle :personas seat :name]))

(defn- say [state seat line]
  (str (persona-name state seat) " (" (name seat) "): \"" line "\""))

(defn- questions-banner [{:keys [questions-left]}]
  (if (pos? questions-left)
    [(str "Questions left: " questions-left)]
    ["No questions left. Who is the Agent? accuse <passenger>"]))

(defn- boarding-lines [state]
  (for [[seat prop polarity] (get-in state [:puzzle :opening])]
    (say state seat (english/render-statement prop polarity (english-context state seat)))))

(defn start [puzzle]
  (let [state {:puzzle puzzle
               :questions-left 3
               :facts (:opening puzzle)
               :live-worlds (logic/consistent (:opening puzzle))
               :over? false}]
    {:state state
     :output (concat ["LAST TRAIN"
                      "Operator: \"One passenger is an Agent. The Agent always lies. Everyone else tells the truth. Find the Agent.\""]
                     (boarding-lines state)
                     (questions-banner state)
                     [syntax])}))

(defn score [win? unused-questions]
  (if win? (+ 100 (* 25 unused-questions)) 0))

(defn- find-seat [state who]
  (english/resolve-seat who (english-context state)))

(defn- reject [state]
  {:state state :output [noisy (str "Questions left: " (:questions-left state))]})

(defn- ask [state target question]
  (if-let [prop (english/parse-question question (english-context state target))]
    (let [yes? (logic/can-say? (get-in state [:puzzle :true-world]) target prop)
          fact [target prop yes?]
          state (-> state
                    (update :facts conj fact)
                    (update :live-worlds #(logic/consistent [fact] %))
                    (update :questions-left dec))]
      {:state state
       :output (cons (say state target (english/render-answer prop yes? (english-context state target)))
                     (questions-banner state))})
    (reject state)))

(defn- accuse [state seat]
  (let [agent (logic/agent-seat (get-in state [:puzzle :true-world]))
        win? (= agent seat)
        outcome {:win? win? :score (score win? (:questions-left state))}]
    {:state (assoc state :over? true :outcome outcome)
     :output [(if win?
                (str "You pull the emergency brake. " (persona-name state agent)
                     "'s face flickers... and the Agent is gone. WIN")
                (str "Wrong passenger. " (persona-name state agent)
                     " stands up and adjusts its tie: \"Mister... Anderson.\" LOSE"))
              (str "Score: " (:score outcome))
              "GAME OVER"]}))

(defn handle
  "Advance the game by one line of player input."
  [state input]
  (let [who (english/seat-pattern (english-context state))
        input (str/trim input)]
    (if (:over? state)
      {:state state :output ["The game is over."]}
      (if-let [[_ seat] (re-matches (re-pattern (str "(?i)accuse " who)) input)]
        (accuse state (find-seat state seat))
        (if-let [[_ target question] (re-matches (re-pattern (str "(?i)ask " who ",? (.+)")) input)]
          (if (zero? (:questions-left state))
            {:state state :output ["Operator: \"No more questions. Accuse the Agent.\""]}
            (ask state (find-seat state target) question))
          (update (reject state) :output conj syntax))))))

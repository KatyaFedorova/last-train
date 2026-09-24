(ns last-train.game
  "Pure game loop: rounds, questions, accusation and score, with template voices."
  (:require [clojure.string :as str]
            [last-train.english :as english]
            [last-train.logic :as logic]))

(def ^:private round-titles
  {2 "Station 1" 3 "Déjà vu" 4 "Station 2" 5 "Last Stop"})

(def ^:private noisy "Operator: \"Signal's noisy. Rephrase.\"")

(def syntax "Ask: ask <passenger> <question>   Accuse: accuse <passenger> [ally <passenger>]")

(defn- english-context
  "English context for the puzzle's personas, with self as I/me/you when given."
  ([state] (english-context state nil))
  ([state self] {:names (update-vals (get-in state [:puzzle :personas]) :name) :self self}))

(defn- say [state seat line]
  (str (get-in state [:puzzle :personas seat :name]) " (" (name seat) "): \"" line "\""))

(defn- round-banner [{:keys [round questions-left]}]
  (cond-> [(str "Round " round " - " (round-titles round))]
    (= 3 round) (conj "A black cat walks past. Then it walks past again.")
    (< round 5) (conj (str "Questions left: " questions-left)
                      syntax)
    (= 5 round) (conj "Accuse the Agent: accuse <passenger> [ally <passenger>]")))

(defn- boarding-lines [state]
  (for [[seat prop polarity] (get-in state [:puzzle :opening])]
    (say state seat (english/render-statement prop polarity (english-context state seat)))))

(defn start [puzzle]
  (let [state {:puzzle puzzle
               :round 2
               :questions-left 3
               :facts (:opening puzzle)
               :live-worlds (logic/consistent (:opening puzzle))
               :over? false}]
    {:state state
     :output (concat ["LAST TRAIN"
                      "Operator: \"There's an Agent in your car. It's wearing someone. Find it first.\""
                      "Round 1 - Boarding"]
                     (boarding-lines state)
                     (round-banner state))}))

(defn score [win? ally? unused-questions]
  (if win?
    (+ 100 (if ally? 50 0) (* 25 unused-questions))
    0))

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
                    (update :questions-left dec)
                    (update :round inc))]
      {:state state
       :output (concat [(say state target (english/render-answer prop yes? (english-context state target)))]
                       (round-banner state))})
    (reject state)))

(defn- accuse [state agent ally]
  (let [world (get-in state [:puzzle :true-world])
        win? (= :agent (world agent))
        ally? (boolean (and win? ally (= :awake (world ally))))
        outcome {:win? win? :ally? ally? :score (score win? ally? (:questions-left state))}]
    {:state (assoc state :over? true :outcome outcome)
     :output [(cond
                (not win?) "The Agent stands up and adjusts its tie: \"Mister... Anderson.\" The screen glitches. LOSE"
                ally? "You pull the emergency brake. The Agent's face flickers... and it's gone. Your ally nods. PERFECT RUN"
                :else "You pull the emergency brake. The Agent's face flickers... and it's gone. WIN")
              (str "Score: " (:score outcome))
              "GAME OVER"]}))

(defn handle
  "Advance the game by one line of player input."
  [state input]
  (let [who (english/seat-pattern (english-context state))
        input (str/trim input)]
    (if (:over? state)
      {:state state :output ["The game is over."]}
      (if-let [[_ agent _ ally] (re-matches (re-pattern (str "(?i)accuse " who "( ally " who ")?")) input)]
        (accuse state (find-seat state agent) (some->> ally (find-seat state)))
        (if-let [[_ target question] (re-matches (re-pattern (str "(?i)ask " who ",? (.+)")) input)]
          (if (zero? (:questions-left state))
            {:state state :output ["Operator: \"No more questions. Accuse the Agent.\""]}
            (ask state (find-seat state target) question))
          (update (reject state) :output conj syntax))))))

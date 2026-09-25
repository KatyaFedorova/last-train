(ns last-train.game
  "Pure game loop: a run of generated trains. On each train the player reads
  four lines and names the Agent with one key. A wrong answer or a timeout
  shows why; one hint per run names an honest passenger."
  (:require [clojure.string :as str]
            [last-train.english :as english]
            [last-train.logic :as logic]
            [last-train.puzzles :as puzzles]))

(def trains 3)
(def seconds-per-train 60)

(def prompt "Who is the Agent? Press A, B, C or D.")
(def ^:private noisy "Operator: \"Signal's noisy. Press A, B, C or D, or type hint.\"")

(defn- seat-name [seat] (name seat))

(defn- line-of
  "What seat said on this train, as the player reads it."
  [puzzle seat]
  (let [[_ prop polarity] (first (filter #(= seat (first %)) (:opening puzzle)))]
    (english/render-statement prop polarity {:self seat})))

(defn lines
  "Seat -> the line that passenger says on the current train."
  [state]
  (into {} (for [seat logic/seats] [seat (line-of (:puzzle state) seat)])))

(defn agent-seat [state]
  (logic/agent-seat (get-in state [:puzzle :true-world])))

(defn- train-lines [state]
  (concat [(str "Train " (:train state) " of " trains)]
          (for [seat logic/seats]
            (str (name seat) ": \"" ((lines state) seat) "\""))
          [prompt]))

(defn- deal [state]
  (let [[puzzle seed] (puzzles/generate (:seed state) (puzzles/level (:train state)))]
    (assoc state :puzzle puzzle :seed seed)))

(defn start
  "A new run from an integer seed."
  [seed]
  (let [state (deal {:seed (puzzles/seed seed)
                     :train 1
                     :right 0
                     :score 0
                     :hint? true
                     :over? false})]
    {:state state
     :output (concat ["LAST TRAIN"
                      "Operator: \"One passenger on each train is the Agent. The Agent lies. Everyone else tells the truth.\""]
                     (train-lines state))}))

(defn points
  "Points for a right answer with seconds-left on the clock (nil without a clock)."
  [seconds-left]
  (+ 100 (* 5 (or seconds-left 0))))

(defn- why-not
  "Why guess cannot be the Agent on this train."
  [state guess]
  (let [puzzle (:puzzle state)
        pretend (puzzles/world guess)
        liars (for [[speaker prop polarity] (:opening puzzle)
                    :when (and (not= speaker guess)
                               (not= polarity (logic/can-say? pretend speaker prop)))]
                speaker)
        liar (first (sort-by #(= % (agent-seat state)) liars))]
    (if liar
      (str "If " (seat-name guess) " were the Agent, " (seat-name liar)
           " would be lying as well, and only one passenger lies.")
      (str (seat-name guess) " told the truth, and the Agent never does."))))

(defn- the-lie [state]
  (let [agent (agent-seat state)]
    (str (seat-name agent) "'s line was the lie: \"" ((lines state) agent) "\"")))

(defn- finish-train
  "Record the answer (guess is nil on a timeout), then deal the next train or end the run."
  [state guess seconds-left]
  (let [agent (agent-seat state)
        right? (= agent guess)
        gained (if right? (points seconds-left) 0)
        verdict (cond
                  right? (str "Right! " (seat-name agent) " is the Agent. +" gained)
                  guess (str "Wrong. " (why-not state guess) " " (the-lie state))
                  :else (str "Time's up! " (the-lie state)))
        state (-> state
                  (update :right + (if right? 1 0))
                  (update :score + gained)
                  (assoc :last {:guess guess :agent agent :right? right?}))]
    (if (< (:train state) trains)
      (let [state (deal (update state :train inc))]
        {:state state :output (cons verdict (train-lines state))})
      {:state (assoc state :over? true)
       :output [verdict
                (str "Run over: " (:right state) " of " trains " right.")
                (str "Score: " (:score state))
                "GAME OVER"]})))

(defn- hint [state]
  (if (:hint? state)
    (let [agent (agent-seat state)
          honest (remove #{agent} logic/seats)
          about-agent (filter (fn [seat] (some #(and (= seat (first %)) (some #{agent} (flatten (second %))))
                                               (get-in state [:puzzle :opening])))
                              honest)
          seat (first (concat about-agent honest))]
      {:state (assoc state :hint? false)
       :output [(str "Operator: \"Tip: " (seat-name seat) " is telling the truth.\"")]})
    {:state state :output ["Operator: \"No hints left on this run.\""]}))

(defn- guessed-seat
  "Seat named by input: a seat letter, alone or after accuse/arrest/blame."
  [state input]
  (let [who (-> input str/trim (str/replace #"[.!?]+$" "")
                (str/replace #"(?i)^(?:accuse|arrest|blame)\s+" ""))]
    (english/resolve-seat who {})))

(defn handle
  "Advance the run by one line of player input. opts may carry :seconds-left."
  ([state input] (handle state input {}))
  ([state input {:keys [seconds-left]}]
   (let [command (str/lower-case (str/trim input))]
     (cond
       (:over? state) {:state state :output ["The run is over."]}
       (= "time" command) (finish-train state nil nil)
       (= "hint" command) (hint state)
       :else (if-let [seat (guessed-seat state input)]
               (finish-train state seat seconds-left)
               {:state state :output [noisy]})))))

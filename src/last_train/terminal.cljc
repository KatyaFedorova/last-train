(ns last-train.terminal
  "Pure web terminal session: a game plus the transcript lines the page shows.
  The page only renders :lines and passes typed commands to submit."
  (:require [clojure.string :as str]
            [last-train.game :as game]
            [last-train.puzzles :as puzzles]))

(def ^:private new-game-commands #{"new" "restart" "play again"})

(def ^:private play-again "Type \"new\" to play again.")

(defn- kind [text]
  (cond
    (str/starts-with? text "Operator:") :operator
    (re-find #"^[^\"]+ \([A-D]\): \"" text) :passenger
    (re-find #"WIN$|LOSE$|^Score: |^GAME OVER$" text) :outcome
    :else :system))

(defn- line [text] {:text text :kind (kind text)})

(defn- add-lines [session texts]
  (update session :lines into (map line) texts))

(defn- start [session puzzle]
  (let [{:keys [state output]} (game/start puzzle)]
    (-> session
        (assoc :game state)
        (add-lines output))))

(defn boot
  "Session for the puzzle named puzzle-param, or a random one."
  [{:keys [puzzle-param rand-int]}]
  (let [named (puzzles/by-name puzzle-param)
        unknown? (and (not named) (not (str/blank? puzzle-param)))]
    (start {:lines (if unknown?
                     [{:text (str "Unknown puzzle \"" puzzle-param "\". Boarding a random train.") :kind :system}]
                     [])}
           (or named (puzzles/pick rand-int nil)))))

(defn- play [session input]
  (let [{:keys [state output]} (game/handle (:game session) input)]
    (cond-> (-> session (assoc :game state) (add-lines output))
      (:over? state) (add-lines [play-again]))))

(defn submit
  "Session after the player types input."
  [session input rand-int]
  (let [command (str/lower-case (str/trim input))
        session (update session :lines conj {:text (str "> " input) :kind :player})]
    (cond
      (new-game-commands command)
      (start session (puzzles/pick rand-int (get-in session [:game :puzzle :name])))

      (= "help" command) (add-lines session [game/syntax])

      :else (play session input))))

(defn controls
  "What the page's buttons offer: each passenger, questions left, game over."
  [{:keys [game]}]
  {:passengers (vec (for [seat [:A :B :C :D]]
                      (merge {:seat (name seat)} (get-in game [:puzzle :personas seat]))))
   :questions-left (:questions-left game)
   :over? (:over? game)})

(defn ask-command
  "The command a button sends to ask seat whether about is the Agent."
  [seat about]
  (str "ask " seat " Is " about " the Agent?"))

(defn accuse-command [seat]
  (str "accuse " seat))

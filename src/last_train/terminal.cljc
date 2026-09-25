(ns last-train.terminal
  "Pure web terminal session: a run plus the transcript lines the page shows.
  The page renders :lines and controls, and passes keys and taps to submit."
  (:require [clojure.string :as str]
            [last-train.game :as game]
            [last-train.logic :as logic]))

(def ^:private new-game-commands #{"new" "restart" "play again"})

(def ^:private play-again "Press Enter or type \"new\" to play again.")

(def help "Press A, B, C or D to name the Agent. hint: one tip per run. new: start a new run.")

(defn- kind [text]
  (cond
    (str/starts-with? text "Operator:") :operator
    (re-find #"^[^\"]+ \([A-D]\): \"" text) :passenger
    (re-find #"^(?:Right!|Wrong\.|Time's up!|Run over:|Score: |GAME OVER$)" text) :outcome
    (re-find #"^Train \d+ of \d+$" text) :train
    :else :system))

(defn- line [text] {:text text :kind (kind text)})

(defn- add-lines [session texts]
  (update session :lines into (map line) texts))

(defn- start [session seed]
  (let [{:keys [state output]} (game/start seed)]
    (-> session
        (assoc :game state)
        (add-lines output))))

(defn- parse-seed [param]
  (when (and param (re-matches #"\d{1,9}" (str/trim param)))
    (parse-long (str/trim param))))

(defn boot
  "Session for the run seeded by seed-param (digits), or a random run."
  [{:keys [seed-param rand-int]}]
  (start {:lines []} (or (parse-seed seed-param) (rand-int 1000000000))))

(defn submit
  "Session after the player types or taps input. opts may carry :seconds-left."
  ([session input rand-int] (submit session input rand-int {}))
  ([session input rand-int opts]
   (let [command (str/lower-case (str/trim input))
         session (update session :lines conj {:text (str "> " input) :kind :player})]
     (cond
       (new-game-commands command) (start session (rand-int 1000000000))
       (= "help" command) (add-lines session [help])
       :else (let [{:keys [state output]} (game/handle (:game session) input opts)]
               (cond-> (-> session (assoc :game state) (add-lines output))
                 (:over? state) (add-lines [play-again])))))))

(defn controls
  "What the page shows: each passenger and their line, the run so far,
  and the last answer (with the Agent revealed only once answered)."
  [{:keys [game]}]
  (let [lines (game/lines game)]
    {:passengers (vec (for [seat logic/seats]
                        (merge {:seat (name seat) :line (lines seat)}
                               (get-in game [:puzzle :personas seat]))))
     :train (:train game)
     :trains game/trains
     :score (:score game)
     :right (:right game)
     :hint? (:hint? game)
     :seconds game/seconds-per-train
     :last (some-> (:last game) (update :guess #(some-> % name)) (update :agent name))
     :over? (:over? game)}))

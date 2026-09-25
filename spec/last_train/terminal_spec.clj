(ns last-train.terminal-spec
  (:require [speclj.core :refer :all]
            [clojure.string :as str]
            [last-train.game :as game]
            [last-train.terminal :as terminal]))

(defn- first-draw [_] 0)

(defn- boot [param]
  (terminal/boot {:seed-param param :rand-int first-draw}))

(defn- texts [session] (map :text (:lines session)))

(defn- submit [session & inputs]
  (reduce #(terminal/submit %1 %2 first-draw) session inputs))

(defn- new-lines [before after]
  (drop (count (:lines before)) (:lines after)))

(defn- agent-letter [session] (name (game/agent-seat (:game session))))

(describe "Booting the web terminal"
  (it "starts the run for a seed in the link"
    (should= (:output (game/start 42)) (texts (boot "42"))))

  (it "starts a random run without a usable seed"
    (should= (:output (game/start 0)) (texts (boot nil)))
    (should= (:output (game/start 0)) (texts (boot "nope")))))

(describe "Playing"
  (it "echoes the key and marks the lines"
    (let [before (boot "42")
          after (submit before (agent-letter before))
          lines (new-lines before after)]
      (should= [:player :outcome :train :passenger :passenger :passenger :passenger :system] (map :kind lines))
      (should (str/starts-with? (:text (second lines)) "Right! "))))

  (it "passes the seconds left to the score"
    (let [before (boot "42")]
      (should= 150 (get-in (terminal/submit before (agent-letter before) first-draw {:seconds-left 10}) [:game :score]))))

  (it "offers a new run when the run is over"
    (let [session (reduce (fn [s _] (submit s "time")) (boot "42") (range 10))]
      (should= ["Run over: 0 of 10 right." "Score: 0" "GAME OVER" "Press Enter or type \"new\" to play again."]
               (take-last 4 (texts session)))))

  (it "shows help without changing the run"
    (let [before (boot "42")
          after (submit before "help")]
      (should= [{:text "> help" :kind :player} {:text terminal/help :kind :system}] (new-lines before after))
      (should= (:game before) (:game after))))

  (it "starts a new run on new, restart or play again"
    (doseq [command ["new" "restart" "PLAY AGAIN" " New "]]
      (let [session (submit (boot "42") "time" command)]
        (should= 1 (get-in session [:game :train]))
        (should= "LAST TRAIN" (:text (second (new-lines (submit (boot "42") "time") session))))))))

(describe "Controls"
  (it "shows each passenger's letter and line, and nothing else"
    (let [session (boot "42")
          {:keys [passengers train trains hint? seconds last over?]} (terminal/controls session)]
      (should= ["A" "B" "C" "D"] (map :seat passengers))
      (should= (map (game/lines (:game session)) [:A :B :C :D]) (map :line passengers))
      (should= #{:seat :line} (set (mapcat keys passengers)))
      (should= [1 10 true 60 nil false] [train trains hint? seconds last over?])))

  (it "reveals the Agent only after the answer"
    (let [session (boot "42")
          agent (agent-letter session)]
      (should= {:guess "A" :agent agent :right? (= "A" agent)}
               (:last (terminal/controls (submit session "A")))))))

(describe "Hiding the roles"
  (it "never prints a role"
    (let [session (submit (boot "42") "hint" "hello" "help" "A")]
      (doseq [text (texts session)]
        (should-not (re-find #":agent|:human|true-world" text))))))

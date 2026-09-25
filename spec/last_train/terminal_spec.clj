(ns last-train.terminal-spec
  (:require [speclj.core :refer :all]
            [clojure.string :as str]
            [last-train.puzzles :as puzzles]
            [last-train.terminal :as terminal]))

(defn- first-draw [_] 0)

(defn- boot [param]
  (terminal/boot {:puzzle-param param :rand-int first-draw}))

(defn- texts [session] (map :text (:lines session)))

(defn- submit [session & inputs]
  (reduce #(terminal/submit %1 %2 first-draw) session inputs))

(defn- new-lines [before after]
  (drop (count (:lines before)) (:lines after)))

(defn- puzzle-name [session] (get-in session [:game :puzzle :name]))

(def syntax "Ask: ask <passenger> Is <passenger> the Agent?   Accuse: accuse <passenger>")

(describe "Booting the web terminal"
  (it "starts the named puzzle"
    (let [session (boot "reference")]
      (should= "reference" (puzzle-name session))
      (should= "LAST TRAIN" (first (texts session)))
      (should-contain "Vera (A): \"Tomasz is the Agent.\"" (texts session))
      (should-contain "Questions left: 3" (texts session))))

  (it "boards a random train for an unknown puzzle"
    (let [session (boot "nope")]
      (should= {:text "Unknown puzzle \"nope\". Boarding a random train." :kind :system}
               (first (:lines session)))
      (should= "LAST TRAIN" (second (texts session)))
      (should (puzzles/by-name (puzzle-name session)))))

  (it "picks a random puzzle when none is named"
    (should= (:name (puzzles/pick first-draw nil)) (puzzle-name (boot nil)))
    (should= (:name (puzzles/pick first-draw nil)) (puzzle-name (boot "")))))

(describe "Typing commands"
  (it "echoes the command and shows the passenger's answer"
    (let [before (boot "reference")
          after (submit before "ask B Is D the Agent?")]
      (should= [{:text "> ask B Is D the Agent?" :kind :player}
                {:text "Tomasz (B): \"No. Mr. Grey is not the Agent.\"" :kind :passenger}]
               (take 2 (new-lines before after)))
      (should= 2 (get-in after [:game :questions-left]))))

  (it "ends the game and offers a new one"
    (let [before (submit (boot "reference") "ask C Is A the Agent?")
          lines (new-lines before (submit before "accuse A"))]
      (should= [:player :outcome :outcome :outcome :system] (map :kind lines))
      (should= "Type \"new\" to play again." (:text (last lines)))
      (should (str/ends-with? (:text (second lines)) "WIN"))))

  (it "keeps offering a new game after it is over"
    (let [session (submit (boot "reference") "accuse D" "accuse A")]
      (should= ["> accuse A" "The game is over." "Type \"new\" to play again."]
               (take-last 3 (texts session)))))

  (it "shows the command syntax on help without changing the game"
    (let [before (boot "reference")
          after (submit before "help")]
      (should= [{:text "> help" :kind :player} {:text syntax :kind :system}]
               (new-lines before after))
      (should= (:game before) (:game after))))

  (it "marks operator lines"
    (let [before (boot "reference")]
      (should= :operator (:kind (second (new-lines before (submit before "hello")))))))

  (it "starts a different puzzle on new, restart or play again, mid-game or after"
    (doseq [command ["new" "restart" "PLAY AGAIN" " New "]]
      (let [mid (submit (boot "reference") command)
            over (submit (boot "reference") "accuse D" command)]
        (should-not= "reference" (puzzle-name mid))
        (should-not= "reference" (puzzle-name over))
        (should= 3 (get-in over [:game :questions-left]))
        (should= "LAST TRAIN" (:text (second (new-lines (submit (boot "reference") "accuse D") over))))))))

(describe "Hiding the roles"
  (it "never prints a role before the game is over"
    (doseq [{:keys [name]} puzzles/catalog]
      (let [session (submit (boot name) "ask A Is B the Agent?" "hello" "help")]
        (doseq [text (texts session)]
          (should-not (re-find #":agent|:human|true-world" text)))))))

(describe "Buttons"
  (it "lists each passenger with seat, name and bio"
    (let [{:keys [passengers questions-left over?]} (terminal/controls (boot "reference"))]
      (should= ["A" "B" "C" "D"] (map :seat passengers))
      (should= {:seat "D" :name "Mr. Grey" :bio "man in a grey suit with a newspaper"} (last passengers))
      (should= 3 questions-left)
      (should= false over?)))

  (it "builds commands the game understands"
    (let [session (submit (boot "reference")
                          (terminal/ask-command "C" "A")
                          (terminal/accuse-command "A"))]
      (should-contain "Ilse (C): \"Yes. Vera is the Agent.\"" (texts session))
      (should (:over? (terminal/controls session)))
      (should= {:win? true :score 150} (get-in session [:game :outcome])))))

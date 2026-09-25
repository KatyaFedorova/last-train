(ns last-train.game-spec
  (:require [speclj.core :refer :all]
            [clojure.string :as str]
            [last-train.game :as game]
            [last-train.puzzles :as puzzles]))

(defn- play [& inputs]
  (reduce (fn [{:keys [state output]} input]
            (let [step (game/handle state input)]
              {:state (:state step) :output (into output (:output step)) :last (:output step)}))
          (assoc (game/start puzzles/reference) :last [])
          inputs))

(defn- text [lines] (str/join "\n" lines))
(def initial-game (game/start puzzles/reference))
(def noisy "Operator: \"Signal's noisy. Rephrase.\"")

(describe "Starting a game"
  (it "explains the rules in one line"
    (should-contain "Operator: \"One passenger is an Agent. The Agent always lies. Everyone else tells the truth. Find the Agent.\""
                    (:output initial-game)))

  (it "shows each passenger's boarding statement"
    (should-contain "Vera (A): \"Tomasz is the Agent.\"" (:output initial-game))
    (should-contain "Tomasz (B): \"Vera is the Agent.\"" (:output initial-game))
    (should-contain "Ilse (C): \"I am not the Agent.\"" (:output initial-game))
    (should-contain "Mr. Grey (D): \"Ilse is not the Agent.\"" (:output initial-game)))

  (it "starts with three questions and shows how to play"
    (should= 3 (get-in initial-game [:state :questions-left]))
    (should-contain "Questions left: 3" (:output initial-game))
    (should-contain game/syntax (:output initial-game)))

  (it "knows the suspects left by the opening statements"
    (should= 2 (count (get-in initial-game [:state :live-worlds]))))

  (it "does not reveal roles"
    (should-not (re-find #"AGENT|HUMAN|:agent|:human" (text (:output initial-game))))))

(describe "Asking questions"
  (it "answers with the passenger's template line"
    (let [{:keys [state last]} (play "ask C is Vera the Agent?")]
      (should= ["Ilse (C): \"Yes. Vera is the Agent.\"" "Questions left: 2"] last)
      (should= 2 (:questions-left state))
      (should= 1 (count (:live-worlds state)))))

  (it "lets the Agent lie"
    (should-contain "Vera (A): \"Yes. Tomasz is the Agent.\"" (:last (play "ask A is B the Agent?"))))

  (it "accepts persona names and you"
    (should-contain "Vera (A): \"No. I am not the Agent.\"" (:last (play "ask vera are you the agent?"))))

  (it "accepts multi-word persona names in any case"
    (should-contain "Mr. Grey (D): \"Yes. Vera is the Agent.\"" (:last (play "ask MR. GREY, is Vera the Agent?"))))

  (it "asks for an accusation after the third question"
    (let [{:keys [state last]} (play "ask C is A the Agent?" "ask D is A the Agent?" "ask B is A the Agent?")]
      (should= 0 (:questions-left state))
      (should-contain "No questions left. Who is the Agent? accuse <passenger>" last)
      (should-not-contain "Questions left: 0" last)))

  (it "refuses questions when none are left without spending anything"
    (let [{:keys [state last]} (play "ask C is A the Agent?" "ask D is A the Agent?" "ask B is A the Agent?"
                                     "ask A is B the Agent?")]
      (should= 0 (:questions-left state))
      (should= ["Operator: \"No more questions. Accuse the Agent.\""] last)))

  (it "rejects noisy text without spending a question"
    (let [{:keys [state last]} (play "ask B what is love?")]
      (should= [noisy "Questions left: 3"] last)
      (should= 3 (:questions-left state))))

  (it "shows the command syntax after an unrecognised command"
    (let [{:keys [state last]} (play "hello")]
      (should= [noisy "Questions left: 3" game/syntax] last)
      (should= (:state initial-game) state)))

  (it "rejects questions to unknown passengers or in unknown forms"
    (should-contain noisy (:last (play "ask Neo is D the Agent?")))
    (should-contain noisy (:last (play "ask B is D the Agent and is C human?")))))

(describe "Accusing"
  (it "loses on a wrong accusation and names the real Agent"
    (let [{:keys [state last]} (play "accuse D")]
      (should= "Wrong passenger. Vera stands up and adjusts its tie: \"Mister... Anderson.\" LOSE" (first last))
      (should-contain "Score: 0" last)
      (should-contain "GAME OVER" last)
      (should= {:win? false :score 0} (:outcome state))))

  (it "wins with the correct Agent and scores unused questions"
    (let [{:keys [state last]} (play "ask C is A the Agent?" "accuse vera")]
      (should-contain "WIN" (text last))
      (should-contain "Score: 150" last)
      (should= {:win? true :score 150} (:outcome state))))

  (it "ignores input after the game is over"
    (let [{:keys [state last]} (play "accuse D" "accuse A")]
      (should= {:win? false :score 0} (:outcome state))
      (should-contain "The game is over." last)))

  (it "treats an accusation of an unknown passenger as noise"
    (let [{:keys [state last]} (play "accuse Neo")]
      (should-not (:over? state))
      (should-contain noisy last))))

(describe "Scoring"
  (it "adds the win and unused questions"
    (should= 0 (game/score false 3))
    (should= 100 (game/score true 0))
    (should= 175 (game/score true 3))))

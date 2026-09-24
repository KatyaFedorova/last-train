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

(describe "Reference puzzle"
  (it "is the verified curated puzzle"
    (should= {:A :agent :B :sleeper :C :sleeper :D :awake} (:true-world puzzles/reference))
    (should= [[:A [:count-eq :agent 0] true]
              [:B [:not [:is :A :agent]] true]
              [:C [:same :B :C] true]
              [:D [:same :B :C] true]]
             (:opening puzzles/reference))
    (should= "Mr. Grey" (get-in puzzles/reference [:personas :D :name]))))

(describe "Starting a game"
  (it "shows each passenger's boarding statement"
    (should-contain "Vera (A): \"There are no Agents on this train.\"" (:output initial-game))
    (should-contain "Tomasz (B): \"Vera is not an Agent.\"" (:output initial-game))
    (should-contain "Ilse (C): \"Tomasz and I are the same kind.\"" (:output initial-game))
    (should-contain "Mr. Grey (D): \"Tomasz and Ilse are the same kind.\"" (:output initial-game)))

  (it "opens Station 1 with three questions"
    (should= 2 (get-in initial-game [:state :round]))
    (should= 3 (get-in initial-game [:state :questions-left]))
    (should-contain "Round 2 - Station 1" (:output initial-game))
    (should-contain "Questions left: 3" (:output initial-game)))

  (it "knows the worlds left by the opening statements"
    (should= 3 (count (get-in initial-game [:state :live-worlds]))))

  (it "does not reveal roles"
    (should-not (re-find #"AGENT|AWAKE|SLEEPER|(?i)is the agent" (text (:output initial-game))))))

(describe "Asking questions"
  (it "answers with the passenger's template line and advances to déjà vu"
    (let [{:keys [state last]} (play "ask B is D an Agent?")]
      (should-contain "Tomasz (B): \"No. Mr. Grey is not an Agent.\"" last)
      (should-contain "Round 3 - Déjà vu" last)
      (should-contain "A black cat walks past. Then it walks past again." last)
      (should-contain "Questions left: 2" last)
      (should= 3 (:round state))
      (should= 2 (:questions-left state))
      (should= 2 (count (:live-worlds state)))))

  (it "accepts persona names and you"
    (let [{:keys [last]} (play "ask vera are you an agent?")]
      (should-contain "Vera (A): \"No. I am not an Agent.\"" last)))

  (it "accepts multi-word persona names in any case"
    (let [{:keys [last]} (play "ask MR. GREY, is Vera an Agent?")]
      (should-contain "Mr. Grey (D): \"Yes. Vera is an Agent.\"" last)))

  (it "reaches the Last Stop after the third question"
    (let [{:keys [state last]} (play "ask B is D an Agent?" "ask B is C an Agent?" "ask D is A an Agent?")]
      (should-contain "Mr. Grey (D): \"Yes. Vera is an Agent.\"" last)
      (should= 5 (:round state))
      (should= 0 (:questions-left state))
      (should-contain "Round 5 - Last Stop" last)
      (should-contain "Accuse the Agent: accuse <passenger> [ally <passenger>]" last)
      (should-not-contain "Questions left: 0" last)))

  (it "labels the second station after the second question"
    (let [{:keys [last]} (play "ask B is D an Agent?" "ask B is C an Agent?")]
      (should-contain "Round 4 - Station 2" last)
      (should-contain "Questions left: 1" last)))

  (it "refuses questions at the Last Stop without spending anything"
    (let [{:keys [state last]} (play "ask B is D an Agent?" "ask B is C an Agent?" "ask D is A an Agent?"
                                     "ask A is B an Agent?")]
      (should= 0 (:questions-left state))
      (should-contain "Operator: \"No more questions. Accuse the Agent.\"" last)))

  (it "rejects noisy text without spending a question"
    (let [{:keys [state last]} (play "what is love?")]
      (should-contain "Operator: \"Signal's noisy. Rephrase.\"" last)
      (should-contain "Questions left: 3" last)
      (should= 2 (:round state))
      (should= 3 (:questions-left state))))

  (it "shows the command syntax after an unrecognised command"
    (let [{:keys [state last]} (play "hello")]
      (should= ["Operator: \"Signal's noisy. Rephrase.\""
                "Questions left: 3"
                "Ask: ask <passenger> <question>   Accuse: accuse <passenger> [ally <passenger>]"]
               last)
      (should= (:state initial-game) state)))

  (it "does not repeat the syntax for an unsupported question"
    (should= ["Operator: \"Signal's noisy. Rephrase.\"" "Questions left: 3"]
             (:last (play "ask B what is love?"))))

  (it "rejects questions to unknown passengers or in unknown forms"
    (should-contain "Operator: \"Signal's noisy. Rephrase.\"" (:last (play "ask Neo is D an Agent?")))
    (should-contain "Operator: \"Signal's noisy. Rephrase.\"" (:last (play "ask B is D an Agent and is C awake?")))))

(describe "Accusing"
  (it "loses on a wrong accusation"
    (let [{:keys [state last]} (play "accuse D")]
      (should-contain "LOSE" (text last))
      (should-contain "Score: 0" last)
      (should-contain "GAME OVER" last)
      (should (:over? state))))

  (it "wins with the correct Agent and scores unused questions"
    (let [{:keys [state last]} (play "accuse vera")]
      (should-contain "WIN" (text last))
      (should-not-contain "PERFECT RUN" (text last))
      (should-contain "Score: 175" last)
      (should= {:win? true :ally? false :score 175} (:outcome state))))

  (it "names the ally by persona name"
    (should= {:win? true :ally? true :score 175}
             (:outcome (:state (play "ask B is D an Agent?" "ask B is C an Agent?" "accuse Vera ally mr. grey")))))

  (it "scores a wrong ally as a plain win"
    (should= {:win? true :ally? false :score 150}
             (:outcome (:state (play "ask B is D an Agent?" "accuse A ally B")))))

  (it "makes a perfect run with the correct Agent and Awake ally"
    (let [{:keys [state last]} (play "ask B is D an Agent?" "ask B is C an Agent?" "accuse A ally D")]
      (should-contain "PERFECT RUN" (text last))
      (should-contain "Score: 175" last)
      (should= {:win? true :ally? true :score 175} (:outcome state))))

  (it "ignores input after the game is over"
    (let [{:keys [state last]} (play "accuse D" "accuse A")]
      (should= {:win? false :ally? false :score 0} (:outcome state))
      (should-contain "The game is over." last)))

  (it "treats an accusation of an unknown passenger as noise"
    (let [{:keys [state last]} (play "accuse Neo")]
      (should-not (:over? state))
      (should-contain "Operator: \"Signal's noisy. Rephrase.\"" last))))

(describe "Scoring"
  (it "adds win, ally and unused questions"
    (should= 0 (game/score false false 3))
    (should= 100 (game/score true false 0))
    (should= 150 (game/score true true 0))
    (should= 175 (game/score true true 1))
    (should= 225 (game/score true true 3))))

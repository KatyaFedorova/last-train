(ns last-train.game-spec
  (:require [speclj.core :refer :all]
            [clojure.string :as str]
            [last-train.game :as game]
            [last-train.logic :as logic]))

(def started (game/start 42))
(def state (:state started))

(defn- agent-letter [state] (name (game/agent-seat state)))
(defn- wrong-letter [state] (name (first (remove #{(game/agent-seat state)} logic/seats))))
(defn- persona [state seat] (get-in state [:puzzle :personas seat :name]))

(defn- play
  "State and last output after inputs; :agent and :wrong stand for this train's letters."
  [state & inputs]
  (reduce (fn [{:keys [state]} input]
            (let [input (case input :agent (agent-letter state) :wrong (wrong-letter state) input)]
              (game/handle state input)))
          {:state state :output []}
          inputs))

(describe "Starting a run"
  (it "explains the rules and shows the first train"
    (let [out (:output started)]
      (should= "LAST TRAIN" (first out))
      (should-contain "Operator: \"One passenger on each train is the Agent. The Agent lies. Everyone else tells the truth.\"" out)
      (should-contain "Train 1 of 10" out)
      (should= 4 (count (filter #(re-matches #"^.+ \([A-D]\): \".+\"$" %) out)))
      (should= game/prompt (last out))))

  (it "starts at train 1 with no score and a hint"
    (should= [1 0 0 true false] ((juxt :train :right :score :hint? :over?) state)))

  (it "deals a train whose lines leave exactly one suspect"
    (should= [(get-in state [:puzzle :true-world])] (logic/consistent (get-in state [:puzzle :opening]))))

  (it "replays the same run from the same seed"
    (should= started (game/start 42)))

  (it "does not reveal the Agent"
    (should-not (re-find #":agent|:human|(?i)right!|wrong\." (str/join "\n" (:output started))))))

(describe "Naming the Agent"
  (it "scores a right answer and deals the next train"
    (let [{s :state out :output} (play state :agent)]
      (should= (str "Right! " (persona state (game/agent-seat state)) " is the Agent. +100") (first out))
      (should-contain "Train 2 of 10" out)
      (should= [2 1 100] ((juxt :train :right :score) s))
      (should= {:guess (game/agent-seat state) :agent (game/agent-seat state) :right? true} (:last s))))

  (it "adds 5 points per second left on the clock"
    (should= 150 (:score (:state (game/handle state (agent-letter state) {:seconds-left 10})))))

  (it "accepts the letter in any case, the name, or accuse"
    (let [seat (game/agent-seat state)]
      (doseq [input [(str/lower-case (name seat)) (persona state seat) (str "accuse " (persona state seat) ".")]]
        (should= 1 (:right (:state (game/handle state input)))))))

  (it "explains a wrong answer and shows the Agent's lie"
    (let [{s :state out :output} (play state :wrong)
          agent (game/agent-seat state)]
      (should (str/starts-with? (first out) "Wrong. "))
      (should (str/includes? (first out) (str (persona state agent) "'s line was the lie: \"" ((game/lines state) agent) "\"")))
      (should= [2 0 0] ((juxt :train :right :score) s))))

  (it "names the lie on a timeout"
    (let [{s :state out :output} (play state "time")]
      (should (str/starts-with? (first out) "Time's up! "))
      (should= {:guess nil :agent (game/agent-seat state) :right? false} (:last s))))

  (it "treats anything else as noise"
    (let [{s :state out :output} (play state "what is love?")]
      (should= ["Operator: \"Signal's noisy. Press A, B, C or D, or type hint.\""] out)
      (should= state s))))

(describe "Explaining a wrong answer"
  (it "names a passenger who would also be lying"
    (let [st (assoc state :puzzle {:true-world {:A :agent :B :human :C :human :D :human}
                                   :personas {:A {:name "Vera"} :B {:name "Tomasz"} :C {:name "Ilse"} :D {:name "Grey"}}
                                   :opening [[:A [:is :B :agent] true]
                                             [:B [:is :A :agent] true]
                                             [:C [:is :A :agent] true]
                                             [:D [:is :D :agent] false]]})]
      (should= "Wrong. If Tomasz were the Agent, Ilse would be lying as well, and only one passenger lies. Vera's line was the lie: \"Tomasz is the Agent.\""
               (first (:output (game/handle st "B"))))
      (should= "Wrong. Grey told the truth, and the Agent never does. Vera's line was the lie: \"Grey is the Agent.\""
               (first (:output (game/handle (assoc-in st [:puzzle :opening] [[:A [:is :D :agent] true]
                                                                           [:B [:is :C :agent] false]
                                                                           [:C [:is :C :agent] false]
                                                                           [:D [:is :B :agent] false]])
                                            "D")))))))

(describe "Hints"
  (it "names an honest passenger once per run"
    (let [{s :state out :output} (play state "hint")
          [_ who] (re-matches #"^Operator: \"Tip: (.+) is telling the truth\.\"$" (first out))
          seat (first (filter #(= who (persona state %)) logic/seats))]
      (should seat)
      (should-not= (game/agent-seat state) seat)
      (should= false (:hint? s))
      (should= ["Operator: \"No hints left on this run.\""] (:output (game/handle s "hint"))))))

(describe "Ending a run"
  (it "ends after ten trains with the tally and score"
    (let [{s :state out :output} (apply play state (concat (repeat 7 :agent) (repeat 3 :wrong)))]
      (should (:over? s))
      (should= ["Run over: 7 of 10 right." "Score: 700" "GAME OVER"] (rest out))
      (should= ["The run is over."] (:output (game/handle s "A")))))

  (it "scores points per right answer"
    (should= 100 (game/points nil))
    (should= 200 (game/points 20))))

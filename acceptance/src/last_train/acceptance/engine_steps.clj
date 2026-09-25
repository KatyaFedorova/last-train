(ns last-train.acceptance.engine-steps
  "Step handlers that drive the logic engine directly."
  (:require [clojure.string :as str]
            [last-train.acceptance.runtime :refer [check]]
            [last-train.english :as english]
            [last-train.game :as game]
            [last-train.logic :as logic]))

(def reference-statements
  "The reference opening statements, as written in the reference-puzzle feature."
  [["A" "B is the Agent" true]
   ["B" "A is the Agent" true]
   ["C" "A is the Agent" true]
   ["D" "D is the Agent" false]])

(defn seat [text]
  (let [seat (keyword (str/trim text))]
    (check (some #{seat} logic/seats) (str "Unknown seat: " text))
    seat))

(defn seat-list
  "Seats named in text such as \"A\", \"A and C\" or \"A, C, and D\"."
  [text]
  (mapv seat (remove str/blank? (str/split text #",\s*(?:and\s+)?|\s+and\s+"))))

(defn yes-no [text]
  (case text
    "yes" true
    "no" false
    (check false (str "Expected yes or no, got: " text))))

(defn prop [text]
  (let [parsed (english/parse-prop text {})]
    (check parsed (str "Unreadable proposition: " text))
    parsed))

(defn- canonical-prop [text]
  (let [parsed (prop text)
        canonical (str/replace (english/render-statement parsed true {}) #"\.$" "")]
    (check (= text canonical) (str "Use canonical proposition text: " canonical))
    parsed))

(defn- role [text]
  (let [role (keyword (str/lower-case text))]
    (check (= text (str/upper-case (name role))) (str "Use uppercase role name: " text))
    (check (some #{role} logic/roles) (str "Unknown role: " text))
    role))

(defn- reference-facts []
  (mapv (fn [[speaker text polarity]] [(seat speaker) (prop text) polarity]) reference-statements))

(defn- set-passenger-world [world a b c d]
  (let [passengers (zipmap logic/seats (map role [a b c d]))]
    (assoc world :world passengers)))

(defn- check-composition [world agents humans]
  (check (some #{(:world world)} logic/all-worlds) (str "Not a possible world: " (:world world)))
  (check (= {:agent (parse-long agents) :human (parse-long humans)}
            (frequencies (vals (:world world))))
         (str "Wrong composition: " (:world world)))
  world)

(defn- all-worlds [world n]
  (check (= (parse-long n) (count logic/all-worlds)) (str "Engine has " (count logic/all-worlds) " worlds"))
  (assoc world :worlds logic/all-worlds))

(defn ask-engine [world speaker text]
  (let [speaker (seat speaker)
        prop (canonical-prop text)
        answer (logic/can-say? (:world world) speaker prop)
        fact [speaker prop answer]]
    (cond-> (-> world
                (assoc :answer answer)
                (update :answers (fnil conj []) fact))
      (:worlds world) (update :worlds #(logic/consistent [fact] %)))))

(defn- check-answer [world expected]
  (check (= (yes-no expected) (:answer world))
         (str "Answer was " (if (:answer world) "yes" "no")))
  world)

(defn check-agent-seats [worlds expected]
  (check (= (seat-list expected) (logic/agent-seats worlds))
         (str "Possible Agent seats were " (mapv name (logic/agent-seats worlds)))))

(defn- reference-evaluation
  "Boolean value of prop computed directly from the world's seat roles."
  [prop world]
  (let [[op x y] prop]
    (case op
      :is (= y (get world x))
      :not (not (reference-evaluation x world))
      :and (every? #(reference-evaluation % world) [x y])
      :or (boolean (some #(reference-evaluation % world) [x y])))))

(def ^:private atomic-props
  (for [seat logic/seats role logic/roles] [:is seat role]))

(def ^:private sample-props
  (concat atomic-props
          (map #(vector :not %) atomic-props)
          (for [x atomic-props y atomic-props] [:and x y])
          (for [x atomic-props y atomic-props] [:or x y])))

(defn- evaluate-props [world]
  (check (= #{:is :not :and :or} (set (map first sample-props))) "Missing operators")
  (assoc world :evaluations (for [w (:worlds world) p sample-props]
                              [p w (logic/evaluate p w)])))

(defn- check-evaluations [world]
  (doseq [[p w value] (:evaluations world)]
    (check (= (reference-evaluation p w) value) (str "Wrong value for " p " in " w)))
  (check (seq (:evaluations world)) "Nothing was evaluated")
  world)

(def ^:private question-props (for [seat logic/seats] [:is seat :agent]))

(defn- agent-and-human-answers [world]
  (assoc world :answer-pairs
         (for [w (:worlds world) p (concat question-props sample-props)
               human (remove #{(logic/agent-seat w)} logic/seats)]
           [(logic/can-say? w (logic/agent-seat w) p) (logic/can-say? w human p)])))

(defn- check-opposite-answers [world]
  (check (seq (:answer-pairs world)) "No answers were collected")
  (check (every? (fn [[agent awake]] (not= agent awake)) (:answer-pairs world))
         "The Agent and a human answered alike")
  world)

(defn- reference-opening [world]
  (let [facts (reference-facts)]
    (assoc world :facts facts :worlds (logic/consistent facts))))

(defn- run-trains [seed]
  (->> (iterate (fn [state] (:state (game/handle state "time"))) (:state (game/start seed)))
       (take game/trains)
       (mapv :puzzle)))

(defn- seeded-run [world seed]
  (assoc world :trains (run-trains (parse-long seed))))

(defn- check-run-puzzles [world]
  (check (= game/trains (count (:trains world))) "The run is not ten trains long")
  (doseq [puzzle (:trains world)]
    (check (= [(:true-world puzzle)] (logic/consistent (:opening puzzle)))
           (str "A train leaves more than one suspect: " (:opening puzzle))))
  world)

(defn- check-replay [world seed]
  (check (= (:trains world) (run-trains (parse-long seed))) "The run did not replay")
  world)

(def handlers
  [[#"^the run seeded (\S+)$" seeded-run]
   [#"^every train of the run leaves exactly one possible Agent$" check-run-puzzles]
   [#"^a second run seeded (\d+) deals the same trains$" check-replay]
   [#"^the (?:true )?passenger world is A=([^,\s]+), B=([^,\s]+), C=([^,\s]+), D=([^,\s]+)$" set-passenger-world]
   [#"^the world has exactly (\d+) Agent and (\d+) humans$" check-composition]
   [#"^the (\d+) possible passenger worlds$" all-worlds]
   [#"^the answer is (\S+)$" check-answer]
   [#"^I ask passenger (\S+) whether (.+)$" ask-engine]
   [#"^I evaluate Is, Not, And, and Or propositions in each world$" evaluate-props]
   [#"^each proposition has its ordinary boolean value in that world$" check-evaluations]
   [#"^the Agent and a human passenger answer the same supported proposition in each world$" agent-and-human-answers]
   [#"^their answers are opposite in every world$" check-opposite-answers]
   ;; The step data table is not carried by the APS IR; it holds the reference statements.
   [#"^the opening statements are(?: the reference statements)?$" reference-opening]
   [#"^I inspect the possible worlds$" (fn [world] (assoc world :worlds (logic/consistent (:facts world))))]
   [#"^there are exactly (\d+) possible worlds$"
    (fn [world n]
      (check (= (parse-long n) (count (:worlds world))) (str "There were " (count (:worlds world)) " worlds"))
      world)]
   [#"^the (?:only )?possible Agent seats? (?:are|is) (.+)$"
    (fn [world expected] (check-agent-seats (:worlds world) expected) world)]])

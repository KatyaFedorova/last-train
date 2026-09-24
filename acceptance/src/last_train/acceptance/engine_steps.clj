(ns last-train.acceptance.engine-steps
  "Step handlers that drive the logic engine directly."
  (:require [clojure.string :as str]
            [last-train.acceptance.runtime :refer [check]]
            [last-train.english :as english]
            [last-train.logic :as logic]))

(def reference-statements
  "The reference opening statements, as written in the reference-puzzle feature."
  [["A" "exactly 0 passengers are Agents" true]
   ["B" "A is not an Agent" true]
   ["C" "B and C are the same kind" true]
   ["D" "B and C are the same kind" true]])

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

(defn- check-composition [world agents awake sleepers]
  (check (some #{(:world world)} logic/all-worlds) (str "Not a possible world: " (:world world)))
  (check (= {:agent (parse-long agents) :awake (parse-long awake) :sleeper (parse-long sleepers)}
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

(defn- check-answer-about [world speaker expected target]
  (let [[asked prop answer] (peek (:answers world))]
    (check (= (seat speaker) asked) (str "Last question went to " asked))
    (check (some #{(seat target)} (flatten prop)) (str "Last question was about " prop))
    (check (= (yes-no expected) answer) (str "Answer was " (if answer "yes" "no")))
    world))

(defn check-agent-seats [worlds expected]
  (check (= (seat-list expected) (logic/agent-seats worlds))
         (str "Possible Agent seats were " (mapv name (logic/agent-seats worlds)))))

(defn- reference-evaluation
  "Boolean value of prop computed directly from the world's seat roles."
  [prop world]
  (let [[op x y] prop]
    (case op
      :is (= y (get world x))
      :same (= (get world x) (get world y))
      :count-eq (= y (count (filter #(= x %) (vals world))))
      :not (not (reference-evaluation x world))
      :and (every? #(reference-evaluation % world) [x y])
      :or (boolean (some #(reference-evaluation % world) [x y])))))

(def ^:private atomic-props
  (concat (for [seat logic/seats role logic/roles] [:is seat role])
          (for [[x y] logic/seat-pairs] [:same x y])
          (for [role logic/roles n (range 5)] [:count-eq role n])))

(def ^:private sample-props
  (concat atomic-props
          (map #(vector :not %) atomic-props)
          (for [x (take 6 atomic-props) y (take-last 6 atomic-props)] [:and x y])
          (for [x (take 6 atomic-props) y (drop 12 (take 18 atomic-props))] [:or x y])))

(defn- evaluate-props [world]
  (check (= #{:is :same :count-eq :not :and :or} (set (map first sample-props))) "Missing operators")
  (assoc world :evaluations (for [w (:worlds world) p sample-props]
                              [p w (logic/evaluate p w)])))

(defn- check-evaluations [world]
  (doseq [[p w value] (:evaluations world)]
    (check (= (reference-evaluation p w) value) (str "Wrong value for " p " in " w)))
  (check (seq (:evaluations world)) "Nothing was evaluated")
  world)

(def ^:private question-props (distinct (map second logic/questions)))

(defn- agent-and-awake-answers [world]
  (assoc world :answer-pairs
         (for [w (:worlds world) p (concat question-props sample-props)]
           (let [by-role (fn [r] (logic/can-say? w (first (filter #(= r (w %)) logic/seats)) p))]
             [(by-role :agent) (by-role :awake)]))))

(defn- check-opposite-answers [world]
  (check (seq (:answer-pairs world)) "No answers were collected")
  (check (every? (fn [[agent awake]] (not= agent awake)) (:answer-pairs world))
         "Agent and Awake answered alike")
  world)

(defn- ask-each-sleeper [world text]
  (let [p (canonical-prop text)]
    (assoc world :sleeper-answers
           (for [w (:worlds world) s logic/seats :when (= :sleeper (w s))]
             (logic/can-say? w s p)))))

(defn- check-every-answer [world expected]
  (check (= 24 (count (:sleeper-answers world))) "Expected two Sleepers in each of 12 worlds")
  (check (every? #(= (yes-no expected) %) (:sleeper-answers world)) "Sleeper answers differ")
  world)

(defn- reference-opening [world]
  (let [facts (reference-facts)]
    (assoc world :facts facts :worlds (logic/consistent facts))))

(defn- reference-worlds [world]
  (let [world (reference-opening world)]
    (check (= 3 (count (:worlds world))) "Reference opening did not leave three worlds")
    world))

(def handlers
  [[#"^the (?:true )?passenger world is A=([^,\s]+), B=([^,\s]+), C=([^,\s]+), D=([^,\s]+)$" set-passenger-world]
   [#"^the world has exactly (\d+) Agent, (\d+) Awake passenger, and (\d+) Sleepers$" check-composition]
   [#"^the (\d+) possible passenger worlds$" all-worlds]
   [#"^the answer is (\S+)$" check-answer]
   [#"^(\S+) answers (\S+) about (\S+)$" check-answer-about]
   [#"^I evaluate Is, Same, CountEq, Not, And, and Or propositions in each world$" evaluate-props]
   [#"^each proposition has its ordinary boolean value in that world$" check-evaluations]
   [#"^the Agent and Awake passenger answer the same supported proposition in each world$" agent-and-awake-answers]
   [#"^their answers are opposite in every world$" check-opposite-answers]
   [#"^each Sleeper is asked whether (.+)$" ask-each-sleeper]
   [#"^every answer is (\S+)$" check-every-answer]
   ;; The step data table is not carried by the APS IR; it holds the reference statements.
   [#"^the opening statements are(?: the reference statements)?$" reference-opening]
   [#"^I inspect the possible worlds$" (fn [world] (assoc world :worlds (logic/consistent (:facts world))))]
   [#"^there are exactly (\d+) possible worlds$"
    (fn [world n]
      (check (= (parse-long n) (count (:worlds world))) (str "There were " (count (:worlds world)) " worlds"))
      world)]
   [#"^the possible worlds are the three reference worlds$" reference-worlds]
   [#"^the reference puzzle solvability at depth (\S+) is (\S+)$"
    (fn [world depth expected]
      (let [depth (parse-long depth)]
        (check depth "Depth must be a number")
        (check (#{1 2} depth) "The reference puzzle test covers depths 1 and 2")
        (check (= (yes-no expected) (logic/solvable? (:worlds world) depth)) "Solvability differs")
        world))]])

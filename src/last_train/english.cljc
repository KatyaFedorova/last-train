(ns last-train.english
  "Template English for propositions: parse player and table text, render lines.
  A context map holds :names (seat -> persona name) and :self (the speaker or
  the addressed passenger, reachable as I/me/you)."
  (:require [clojure.string :as str]))

(def ^:private kind-words {"the agent" :agent "an agent" :agent "human" :human "a human" :human})
(def ^:private kind-text {:agent "the Agent" :human "human"})

(defn- references [{:keys [names self]}]
  (merge (into {} (for [seat [:A :B :C :D]] [(str/lower-case (name seat)) seat]))
         (into {} (for [[seat persona] names] [(str/lower-case persona) seat]))
         (when self {"i" self "me" self "you" self})))

(defn- regex-quote [word]
  #?(:clj (java.util.regex.Pattern/quote word)
     :cljs (str/replace word #"[.*+?^${}()|\[\]\\]" "\\$&")))

(defn- alternation [words]
  (str "(" (str/join "|" (map regex-quote
                              (sort-by (comp - count) words)))
       ")"))

(defn seat-pattern
  "Regex group matching any word that names a seat in context, longest first."
  [context]
  (alternation (keys (references context))))

(defn resolve-seat
  "Seat named by who in context (seat letter, persona name or pronoun), or nil."
  [who context]
  ((references context) (str/lower-case (str/trim who))))

(defn- normalize [text]
  (-> text str/trim (str/replace #"[.?!]+$" "") str/trim str/lower-case))

(defn- atomic-prop [text refs]
  (let [ref (alternation (keys refs))
        kind (alternation (keys kind-words))]
    (if-let [[_ x negated k] (re-matches (re-pattern (str ref " (?:is|am) (not )?" kind)) text)]
      (let [prop [:is (refs x) (kind-words k)]]
        (if negated [:not prop] prop))
      (when-let [[_ x y] (re-matches (re-pattern (str ref " or " ref " (?:is|am) the agent")) text)]
        [:or [:is (refs x) :agent] [:is (refs y) :agent]]))))

(declare ^:private compound-prop)

(defn- split-prop [text refs connective op]
  (let [parts (str/split text (re-pattern (str " " connective " ")))]
    (some (fn [i]
            (let [left (compound-prop (str/join (str " " connective " ") (take i parts)) refs)
                  right (compound-prop (str/join (str " " connective " ") (drop i parts)) refs)]
              (when (and left right) [op left right])))
          (range 1 (count parts)))))

(defn- compound-prop [text refs]
  (or (atomic-prop text refs)
      (when-let [[_ inner] (re-matches #"it is not true that (.+)" text)]
        (when-let [prop (compound-prop inner refs)] [:not prop]))
      (split-prop text refs "or" :or)
      (split-prop text refs "and" :and)))

(defn parse-prop
  "Proposition expressed by text, or nil when the text cannot be read."
  [text context]
  (compound-prop (normalize text) (references context)))

(defn parse-question
  "Proposition asked by a supported yes/no question, or nil."
  [text context]
  (let [refs (references context)
        ref (seat-pattern context)]
    (when-let [[_ x k] (re-matches (re-pattern (str "(?:is|are|am) " ref " " (alternation (keys kind-words))))
                                   (normalize text))]
      [:is (refs x) (kind-words k)])))

(defn- seat-name [seat {:keys [names self]}]
  (if (= seat self) "I" (get names seat (name seat))))

(defn- is-clause [seat role negated context]
  (let [subject (seat-name seat context)]
    (str subject (if (= "I" subject) " am " " is ") (when negated "not ") (kind-text role))))

(declare ^:private clause)

(defn- either-agent?
  "True for 'X or Y is the Agent' about two other passengers."
  [[op x y] {:keys [self]}]
  (and (= :or op)
       (= :is (first x)) (= :agent (last x))
       (= :is (first y)) (= :agent (last y))
       (not= self (second x)) (not= self (second y))))

(defn- clause [prop context]
  (let [[op x y] prop]
    (cond
      (= :is op) (is-clause x y false context)
      (either-agent? prop context) (str (seat-name (second x) context) " or "
                                        (seat-name (second y) context) " is the Agent")
      (and (= :not op) (= :is (first x))) (is-clause (second x) (last x) true context)
      (= :not op) (str "it is not true that " (clause x context))
      (= :and op) (str (clause x context) " and " (clause y context))
      (= :or op) (str (clause x context) " or " (clause y context)))))

(defn- sentence [text]
  (str (str/upper-case (subs text 0 1)) (subs text 1) "."))

(defn render-statement
  "A sentence asserting prop (polarity true) or its negation."
  [prop polarity context]
  (sentence (clause (if polarity prop [:not prop]) context)))

(defn render-answer [prop yes? context]
  (str (if yes? "Yes. " "No. ") (render-statement prop yes? context)))

(defn render-question [[_ x role] context]
  (str "is " (seat-name x (dissoc context :self)) " " (kind-text role) "?"))

(ns last-train.english
  "Template English for propositions: parse player and table text, render lines.
  A context map holds :names (seat -> persona name) and :self (the speaker or
  the addressed passenger, reachable as I/me/you)."
  (:require [clojure.string :as str]))

(def ^:private kind-singular {"an agent" :agent "awake" :awake "a sleeper" :sleeper})
(def ^:private kind-plural {"agents" :agent "agent" :agent "awake" :awake
                            "sleepers" :sleeper "sleeper" :sleeper})
(def ^:private singular-kind-text {:agent "an Agent" :awake "Awake" :sleeper "a Sleeper"})
(def ^:private plural-kind-text {:agent "Agents" :awake "Awake" :sleeper "Sleepers"})

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
        kind (alternation (keys kind-singular))
        plural (alternation (keys kind-plural))
        negate (fn [negated prop] (if negated [:not prop] prop))]
    (if-let [[_ x negated k] (re-matches (re-pattern (str ref " (?:is|am) (not )?" kind)) text)]
      (negate negated [:is (refs x) (kind-singular k)])
      (if-let [[_ x y negated] (re-matches (re-pattern (str ref " and " ref " are (not )?the same kind")) text)]
        (negate negated [:same (refs x) (refs y)])
        (if-let [[_ n k] (re-matches (re-pattern (str "exactly (\\d+) passengers? (?:are|is) " plural)) text)]
          [:count-eq (kind-plural k) (parse-long n)]
          (when (= "there are no agents on this train" text)
            [:count-eq :agent 0]))))))

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
        ref (seat-pattern context)
        text (normalize text)]
    (if-let [[_ x k] (re-matches (re-pattern (str "(?:is|are|am) " ref " " (alternation (keys kind-singular)))) text)]
      [:is (refs x) (kind-singular k)]
      (when-let [[_ x y] (re-matches (re-pattern (str "are " ref " and " ref " the same kind")) text)]
        [:same (refs x) (refs y)]))))

(defn- seat-name [seat {:keys [names self]}]
  (if (= seat self) "I" (get names seat (name seat))))

(defn- pair-names [x y {:keys [self] :as context}]
  (let [[x y] (if (= x self) [y x] [x y])]
    (str (seat-name x context) " and " (seat-name y context))))

(defn- is-clause [seat role negated context]
  (let [subject (seat-name seat context)]
    (str subject (if (= "I" subject) " am " " is ") (when negated "not ") (singular-kind-text role))))

(declare ^:private clause)

(defn- negated-clause [prop context]
  (let [[op x y] prop]
    (case op
      :is (is-clause x y true context)
      :same (str (pair-names x y context) " are not the same kind")
      (str "it is not true that " (clause prop context)))))

(defn- clause [prop context]
  (let [[op x y] prop]
    (case op
      :is (is-clause x y false context)
      :same (str (pair-names x y context) " are the same kind")
      :count-eq (if (= [:agent 0] [x y])
                  "there are no Agents on this train"
                  (str "exactly " y " passengers are " (plural-kind-text x)))
      :and (str (clause x context) " and " (clause y context))
      :or (str (clause x context) " or " (clause y context))
      :not (negated-clause x context))))

(defn- sentence [text]
  (str (str/upper-case (subs text 0 1)) (subs text 1) "."))

(defn render-statement
  "A sentence asserting prop (polarity true) or its negation."
  [prop polarity context]
  (sentence (clause (if polarity prop [:not prop]) context)))

(defn render-answer [prop yes? context]
  (str (if yes? "Yes. " "No. ") (render-statement prop yes? context)))

(defn render-question [prop context]
  (let [[op x y] prop
        context (dissoc context :self)]
    (case op
      :is (str "is " (seat-name x context) " " (singular-kind-text y) "?")
      :same (str "are " (pair-names x y context) " the same kind?"))))

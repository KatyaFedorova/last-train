(ns last-train.english-spec
  (:require [speclj.core :refer :all]
            [last-train.english :as english]))

(def names {:A "Vera" :B "Tomasz" :C "Ilse" :D "Mr. Grey"})

(describe "Parsing propositions"
  (it "parses kind claims about seats"
    (should= [:is :A :agent] (english/parse-prop "A is an Agent" {}))
    (should= [:is :D :awake] (english/parse-prop "D is Awake" {}))
    (should= [:is :C :sleeper] (english/parse-prop "C is a Sleeper" {})))

  (it "parses negated kind claims"
    (should= [:not [:is :A :agent]] (english/parse-prop "A is not an Agent" {})))

  (it "parses same-kind claims"
    (should= [:same :C :D] (english/parse-prop "C and D are the same kind" {}))
    (should= [:not [:same :C :D]] (english/parse-prop "C and D are not the same kind" {})))

  (it "parses counting claims"
    (should= [:count-eq :agent 0] (english/parse-prop "exactly 0 passengers are Agents" {}))
    (should= [:count-eq :sleeper 2] (english/parse-prop "exactly 2 passengers are Sleepers" {}))
    (should= [:count-eq :agent 0] (english/parse-prop "There are no Agents on this train" {})))

  (it "parses negation, conjunction and disjunction"
    (should= [:not [:is :A :agent]] (english/parse-prop "it is not true that A is an Agent" {}))
    (should= [:and [:is :A :agent] [:is :D :awake]]
             (english/parse-prop "A is an Agent and D is Awake" {}))
    (should= [:or [:is :A :agent] [:is :B :agent]]
             (english/parse-prop "A is an Agent or B is an Agent" {})))

  (it "resolves persona names and the speaker's own pronouns"
    (should= [:is :D :agent] (english/parse-prop "Mr. Grey is an Agent" {:names names}))
    (should= [:same :B :C] (english/parse-prop "Tomasz and I are the same kind" {:names names :self :C}))
    (should= [:not [:is :C :agent]] (english/parse-prop "I am not an Agent" {:names names :self :C})))

  (it "ignores case and trailing punctuation"
    (should= [:is :A :agent] (english/parse-prop "vera is an agent." {:names names})))

  (it "returns nil for text it cannot read"
    (should-be-nil (english/parse-prop "what is love?" {}))
    (should-be-nil (english/parse-prop "Neo is an Agent" {:names names}))
    (should-be-nil (english/parse-prop "I am an Agent" {}))))

(describe "Parsing questions"
  (it "parses kind questions"
    (should= [:is :D :agent] (english/parse-question "is D an Agent?" {}))
    (should= [:is :A :awake] (english/parse-question "Is Vera awake?" {:names names}))
    (should= [:is :C :sleeper] (english/parse-question "is Ilse a Sleeper" {:names names})))

  (it "parses same-kind questions"
    (should= [:same :C :D] (english/parse-question "are C and D the same kind?" {})))

  (it "resolves you to the addressed passenger"
    (should= [:is :A :agent] (english/parse-question "are you an Agent?" {:self :A}))
    (should= [:same :A :B] (english/parse-question "are you and Tomasz the same kind?" {:names names :self :A})))

  (it "rejects anything that is not a single supported question"
    (should-be-nil (english/parse-question "what is love?" {}))
    (should-be-nil (english/parse-question "is A an Agent and is B awake?" {}))
    (should-be-nil (english/parse-question "is Neo an Agent?" {:names names}))))

(describe "Rendering"
  (it "renders statements with persona names"
    (should= "There are no Agents on this train."
             (english/render-statement [:count-eq :agent 0] true {:names names :self :A}))
    (should= "Vera is not an Agent."
             (english/render-statement [:is :A :agent] false {:names names :self :B}))
    (should= "Tomasz and I are the same kind."
             (english/render-statement [:same :B :C] true {:names names :self :C}))
    (should= "Tomasz and Ilse are the same kind."
             (english/render-statement [:same :B :C] true {:names names :self :D})))

  (it "renders other propositions"
    (should= "I am a Sleeper." (english/render-statement [:is :B :sleeper] true {:names names :self :B}))
    (should= "Vera is Awake." (english/render-statement [:is :A :awake] true {:names names}))
    (should= "Vera and Ilse are not the same kind."
             (english/render-statement [:same :A :C] false {:names names}))
    (should= "Exactly 2 passengers are Sleepers."
             (english/render-statement [:count-eq :sleeper 2] true {:names names}))
    (should= "It is not true that exactly 2 passengers are Sleepers."
             (english/render-statement [:count-eq :sleeper 2] false {:names names}))
    (should= "A is an Agent and D is Awake."
             (english/render-statement [:and [:is :A :agent] [:is :D :awake]] true {}))
    (should= "A is an Agent or B is an Agent."
             (english/render-statement [:or [:is :A :agent] [:is :B :agent]] true {})))

  (it "renders answers"
    (should= "No. Mr. Grey is not an Agent."
             (english/render-answer [:is :D :agent] false {:names names :self :B}))
    (should= "Yes. I am an Agent."
             (english/render-answer [:is :B :agent] true {:names names :self :B})))

  (it "renders questions that parse back"
    (should= "is D an Agent?" (english/render-question [:is :D :agent] {}))
    (should= "are C and D the same kind?" (english/render-question [:same :C :D] {}))
    (doseq [prop [[:is :A :sleeper] [:is :B :awake] [:same :A :D]]]
      (should= prop (english/parse-question (english/render-question prop {:names names}) {:names names}))))

  (it "renders statements that parse back"
    (doseq [[prop polarity self] [[[:count-eq :agent 0] true :A]
                                  [[:is :A :agent] false :B]
                                  [[:same :B :C] true :C]
                                  [[:same :B :C] false :D]
                                  [[:is :C :awake] true :C]]]
      (should= (if polarity prop [:not prop])
               (english/parse-prop (english/render-statement prop polarity {:names names :self self})
                                   {:names names :self self})))))


(describe "Naming seats"
  (it "resolves seat letters, persona names and the addressed passenger"
    (should= :A (english/resolve-seat " a " {:names names}))
    (should= :D (english/resolve-seat "MR. GREY" {:names names}))
    (should= :B (english/resolve-seat "you" {:names names :self :B}))
    (should-be-nil (english/resolve-seat "you" {:names names}))
    (should-be-nil (english/resolve-seat "Neo" {:names names})))

  (it "matches the longest seat name first"
    (let [pattern (re-pattern (str "(?i)" (english/seat-pattern {:names {:A "Al" :B "Alma"}})))]
      (should= ["Alma" "Alma"] (re-find pattern "Alma"))
      (should-not (re-matches pattern "Neo")))))

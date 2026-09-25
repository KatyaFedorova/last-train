(ns last-train.english-spec
  (:require [speclj.core :refer :all]
            [last-train.english :as english]))

(def names {:A "Vera" :B "Tomasz" :C "Ilse" :D "Mr. Grey"})

(describe "Parsing propositions"
  (it "parses Agent claims about seats"
    (should= [:is :A :agent] (english/parse-prop "A is the Agent" {}))
    (should= [:is :A :agent] (english/parse-prop "A is an Agent" {}))
    (should= [:is :D :human] (english/parse-prop "D is human" {})))

  (it "parses negated claims"
    (should= [:not [:is :A :agent]] (english/parse-prop "A is not the Agent" {})))

  (it "parses one-of-two claims"
    (should= [:or [:is :B :agent] [:is :C :agent]] (english/parse-prop "B or C is the Agent" {})))

  (it "parses negation, conjunction and disjunction"
    (should= [:not [:is :A :agent]] (english/parse-prop "it is not true that A is the Agent" {}))
    (should= [:and [:is :A :agent] [:is :D :human]]
             (english/parse-prop "A is the Agent and D is human" {}))
    (should= [:or [:is :A :agent] [:is :B :human]]
             (english/parse-prop "A is the Agent or B is human" {})))

  (it "resolves persona names and the speaker's own pronouns"
    (should= [:is :D :agent] (english/parse-prop "Mr. Grey is the Agent" {:names names}))
    (should= [:not [:is :C :agent]] (english/parse-prop "I am not the Agent" {:names names :self :C})))

  (it "ignores case and trailing punctuation"
    (should= [:is :A :agent] (english/parse-prop "vera is the agent." {:names names})))

  (it "returns nil for text it cannot read"
    (should-be-nil (english/parse-prop "what is love?" {}))
    (should-be-nil (english/parse-prop "A is the Agent and the walls are listening" {}))
    (should-be-nil (english/parse-prop "Neo is the Agent" {:names names}))
    (should-be-nil (english/parse-prop "I am the Agent" {}))))

(describe "Rendering"
  (it "renders statements with persona names"
    (should= "Tomasz is the Agent." (english/render-statement [:is :B :agent] true {:names names :self :A}))
    (should= "Vera is not the Agent." (english/render-statement [:is :A :agent] false {:names names :self :B}))
    (should= "I am not the Agent." (english/render-statement [:is :C :agent] false {:names names :self :C}))
    (should= "Tomasz or Ilse is the Agent."
             (english/render-statement [:or [:is :B :agent] [:is :C :agent]] true {:names names :self :A})))

  (it "renders other propositions"
    (should= "Vera is human." (english/render-statement [:is :A :human] true {:names names}))
    (should= "It is not true that Tomasz or Ilse is the Agent."
             (english/render-statement [:or [:is :B :agent] [:is :C :agent]] false {:names names}))
    (should= "A is the Agent and D is human."
             (english/render-statement [:and [:is :A :agent] [:is :D :human]] true {}))
    (should= "I am the Agent or B is the Agent."
             (english/render-statement [:or [:is :A :agent] [:is :B :agent]] true {:self :A})))

  (it "renders statements that parse back"
    (doseq [[prop polarity self] [[[:is :B :agent] true :A]
                                  [[:is :A :agent] false :B]
                                  [[:is :C :agent] false :C]
                                  [[:or [:is :B :agent] [:is :D :agent]] true :A]
                                  [[:is :C :human] true :C]]]
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
      (should-not (re-matches pattern "Neo"))))

  (it "treats regex characters in names literally"
    (let [pattern (re-pattern (english/seat-pattern {:names names}))]
      (should (re-matches pattern "mr. grey"))
      (should-not (re-matches pattern "mrx grey")))))

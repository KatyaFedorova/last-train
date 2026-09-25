(ns last-train.english-property-spec
  (:require [speclj.core :refer :all]
            [clojure.string :as str]
            [last-train.check :refer [passes?]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [last-train.english :as english]
            [last-train.generators :as g]
            [last-train.logic :as logic]))

(def names {:A "Vera" :B "Tomasz" :C "Ilse" :D "Mr. Grey"})
(def gen-context (gen/fmap (fn [self] {:names names :self self}) (gen/one-of [(gen/return nil) g/seat])))

(def ^:private inline-literal
  "A literal whose negation stays inside its clause (\"is not\", \"are not\")."
  (gen/such-that (fn [[op inner]] (or (not= :not op) (= :is (first inner))))
                 g/literal 100))

(def statement
  "A prop and polarity the game can voice unambiguously. \"It is not true that X\"
  does not say how far the negation reaches, so it only appears alone."
  (gen/one-of [(gen/tuple g/literal gen/boolean)
               (gen/tuple (gen/tuple (gen/elements [:and :or]) inline-literal inline-literal)
                          (gen/return true))]))

(defn- equivalent? [p q]
  (every? #(= (logic/evaluate p %) (logic/evaluate q %)) logic/all-worlds))

(describe "English properties"
  (it "reads back every rendered statement as the same claim"
    (should (passes? (prop/for-all [[p polarity] statement ctx gen-context]
                       (let [parsed (english/parse-prop (english/render-statement p polarity ctx) ctx)]
                         (and parsed (equivalent? parsed (if polarity p [:not p]))))))))

  (it "resolves a seat however its name is cased or padded"
    (should (passes? (prop/for-all [seat g/seat upper? gen/boolean pad (gen/elements ["" " " "  "])]
                       (let [n (get names seat)]
                         (= seat (english/resolve-seat (str pad (if upper? (str/upper-case n) (str/lower-case n)) pad)
                                                       {:names names}))))))))

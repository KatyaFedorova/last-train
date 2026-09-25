(ns last-train.generators
  "test.check generators for seats, worlds, propositions and player input."
  (:require [clojure.test.check.generators :as gen]
            [last-train.logic :as logic]))

(def seat (gen/elements logic/seats))
(def role (gen/elements logic/roles))
(def world (gen/elements logic/all-worlds))

(def atom-prop
  (gen/tuple (gen/return :is) seat role))

(def literal
  (gen/one-of [atom-prop (gen/fmap #(vector :not %) atom-prop)]))

(def prop
  "Propositions of the shapes the game voices: literals and one connective over literals."
  (gen/one-of [literal
               (gen/tuple (gen/elements [:and :or]) literal literal)]))

(def fact
  "A fact as a passenger in world w would state it."
  (fn [w] (gen/fmap (fn [[speaker p]] [speaker p (logic/can-say? w speaker p)])
                    (gen/tuple seat prop))))

(def player-line
  "Player input: guesses, hints, timeouts and arbitrary text."
  (gen/one-of [(gen/elements ["A" "b" "C" "d" "hint" "time" "accuse B" "Neo" "help"])
               gen/string-ascii]))

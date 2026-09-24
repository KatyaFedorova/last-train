(ns last-train.acceptance.transcript
  "Reading game transcript lines, shared by the terminal and web step handlers."
  (:require [clojure.string :as str]
            [last-train.acceptance.engine-steps :as engine]
            [last-train.acceptance.runtime :refer [check]]
            [last-train.english :as english]))

(def ^:private statement-line #"^(.+) \(([A-D])\): \"(.*)\"$")

(defn statements [lines]
  (keep #(re-matches statement-line %) lines))

(defn questions-left [lines]
  (some->> lines
           (keep #(second (re-matches #"^Questions left: (\d+)$" %)))
           last
           parse-long))

(defn round-title [lines]
  (some->> lines (keep #(second (re-matches #"^Round \d+ - (.+)$" %))) last))

(defn answers-of
  "Yes/no answers (true/false) given by seat in lines, in order."
  [lines seat]
  (keep (fn [line]
          (when-let [[_ _ speaker answer] (re-matches #"^(.+) \(([A-D])\): \"(Yes|No)\. .*\"$" line)]
            (when (= seat speaker) (= "Yes" answer))))
        lines))

(defn check-reference-statements
  "The statements in lines convey exactly the reference opening statements."
  [lines]
  (let [shown (statements lines)
        names (into {} (for [[_ persona seat] shown] [(keyword seat) persona]))
        conveyed (set (for [[_ _ seat line] shown]
                        [(keyword seat) (english/parse-prop line {:names names :self (keyword seat)})]))
        expected (set (for [[speaker text polarity] engine/reference-statements]
                        (let [p (engine/prop text)] [(engine/seat speaker) (if polarity p [:not p])])))]
    (check (= expected conveyed) (str "Statements conveyed " conveyed))))

(defn check-no-roles [lines]
  (let [text (str/join "\n" lines)]
    (check (not (re-find #"AGENT|AWAKE|SLEEPER" text)) "Role tokens displayed")
    (check (not (re-find #"(?i)is the agent|is your ally|is awake\b" text)) "A role is revealed")))

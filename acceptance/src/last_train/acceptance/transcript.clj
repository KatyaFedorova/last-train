(ns last-train.acceptance.transcript
  "Reading game transcript lines, shared by the terminal and web step handlers."
  (:require [clojure.string :as str]
            [last-train.acceptance.runtime :refer [check]]
            [last-train.english :as english]
            [last-train.logic :as logic]))

(def ^:private statement-line #"^(.+) \(([A-D])\): \"(.*)\"$")

(defn statements [lines]
  (keep #(re-matches statement-line %) lines))

(defn conveyed-facts
  "The [seat prop true] facts the last four statement lines convey."
  [lines]
  (let [shown (take-last 4 (statements lines))
        names (into {} (for [[_ persona seat] shown] [(keyword seat) persona]))]
    (vec (for [[_ _ seat line] shown]
           (let [prop (english/parse-prop line {:names names :self (keyword seat)})]
             (check prop (str "Unreadable line: " line))
             [(keyword seat) prop true])))))

(defn check-one-agent [lines]
  (let [worlds (logic/consistent (conveyed-facts lines))]
    (check (= 1 (count worlds)) (str "The lines leave " (count worlds) " possible worlds"))))

(defn check-no-roles [lines]
  (let [text (str/join "\n" lines)]
    (check (not (re-find #"AGENT|HUMAN|:agent|:human|true-world" text)) "Role tokens displayed")
    (check (not (re-find #"(?m)^(?:Right!|Wrong\.|Time's up!)" text)) "An answer is shown before any guess")))

(defn check-see
  "Text appears in the recent lines."
  [recent text]
  (check (str/includes? (str/join "\n" recent) text)
         (str "Did not see " (pr-str text) " in " (vec recent))))

(defn check-score [recent score]
  (check (some #{(str "Score: " score)} recent) (str "Output: " (vec recent))))

(defn check-game-over-shown [recent]
  (check (some #{"GAME OVER"} recent) "GAME OVER not shown"))

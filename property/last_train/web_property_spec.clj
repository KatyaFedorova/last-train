(ns last-train.web-property-spec
  (:require [speclj.core :refer :all]
            [last-train.check :refer [passes?]]
            [clojure.string :as str]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [last-train.generators :as g]
            [last-train.puzzles :as puzzles]
            [last-train.web :as web])
  (:import (java.net URLEncoder)))

(defn- encode [text] (URLEncoder/encode text "UTF-8"))

(defn- unescape [text]
  (-> text
      (str/replace "&lt;" "<") (str/replace "&gt;" ">") (str/replace "&quot;" "\"")
      (str/replace "&#39;" "'") (str/replace "&amp;" "&")))

(defn- form-moves
  "The moves each form on the page posts back."
  [html]
  (for [[_ form] (re-seq #"(?s)<form[^>]*>(.*?)</form>" html)]
    (map (comp unescape second) (re-seq #"<input type=\"hidden\" name=\"move\" value=\"([^\"]*)\">" form))))

(defn- page-transcript [html]
  (unescape (second (re-find #"(?s)<pre id=\"transcript\">(.*?)</pre>" html))))

(def ^:private field-name (gen/elements ["move" "action" "passenger" "question" "agent" "ally"]))

(describe "Web properties"
  (it "reads back every url-encoded form field in order"
    (should (passes? (prop/for-all [pairs (gen/vector (gen/tuple field-name gen/string) 0 6)]
                       (= (reduce (fn [m [k v]] (update m k (fnil conj []) v)) {} pairs)
                          (web/parse-form (str/join "&" (for [[k v] pairs] (str (encode k) "=" (encode v))))))))))

  (it "shows the same transcript the game played"
    (should (passes? (prop/for-all [moves (gen/vector g/player-line 0 6)]
                       (= (str/join "\n" (:output (web/replay puzzles/reference moves)))
                          (page-transcript (web/page puzzles/reference moves)))))))

  (it "carries every move of a running game back in its forms"
    (should (passes? (prop/for-all [moves (gen/vector g/player-line 0 6)]
                       (or (get-in (web/replay puzzles/reference moves) [:state :over?])
                           (let [carried (form-moves (web/page puzzles/reference moves))]
                             (and (seq carried) (every? #(= moves %) carried)))))))))

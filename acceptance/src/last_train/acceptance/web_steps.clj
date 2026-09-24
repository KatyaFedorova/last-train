(ns last-train.acceptance.web-steps
  "Step handlers that play the game through the web page: they read the
  rendered HTML and submit its own forms back to the request handler."
  (:require [clojure.string :as str]
            [last-train.acceptance.engine-steps :as engine]
            [last-train.acceptance.runtime :refer [check]]
            [last-train.acceptance.transcript :as transcript]
            [last-train.puzzles :as puzzles]
            [last-train.web :as web])
  (:import (java.net URLEncoder)))

(def ^:private seat-values ["A" "B" "C" "D"])

(defn- unescape [text]
  (-> text
      (str/replace "&lt;" "<") (str/replace "&gt;" ">") (str/replace "&quot;" "\"")
      (str/replace "&#39;" "'") (str/replace "&amp;" "&")))

(defn- transcript-lines [html]
  (let [[_ text] (re-find #"(?s)<pre id=\"transcript\">(.*?)</pre>" html)]
    (check text "No transcript on the page")
    (str/split-lines (unescape text))))

(defn- forms
  "Each form on the page as {:hidden [[name value]...] :selects {name [values]} :texts #{names}}."
  [html]
  (for [[_ form] (re-seq #"(?s)<form[^>]*>(.*?)</form>" html)]
    {:hidden (vec (for [[_ n v] (re-seq #"<input type=\"hidden\" name=\"([^\"]+)\" value=\"([^\"]*)\">" form)]
                    [n (unescape v)]))
     :selects (into {} (for [[_ n options] (re-seq #"(?s)<select name=\"([^\"]+)\">(.*?)</select>" form)]
                         [n (mapv second (re-seq #"<option value=\"([^\"]*)\">" options))]))
     :texts (set (map second (re-seq #"<input type=\"text\" name=\"([^\"]+)\"" form)))}))

(defn- form-for [html action]
  (let [form (first (filter #(some #{["action" action]} (:hidden %)) (forms html)))]
    (check form (str "No " action " form on the page"))
    form))

(defn- request [world req]
  (let [{:keys [status body]} (web/respond puzzles/reference req)
        seen (some-> (get-in world [:web :html]) transcript-lines count)]
    (check (= 200 status) (str "Page answered " status))
    (assoc world :web {:html body :seen (or seen 0)})))

(defn open-game [world]
  (request world {:request-method :get :uri "/"}))

(defn- html [world]
  (check (:web world) "No web game is open")
  (get-in world [:web :html]))

(defn- lines [world] (transcript-lines (html world)))
(defn- new-lines [world] (drop (get-in world [:web :seen]) (lines world)))

(defn- encode [text] (URLEncoder/encode text "UTF-8"))

(defn- submit
  "Fill the page's form for action with choices and post it."
  [world action choices]
  (let [{:keys [hidden selects texts]} (form-for (html world) action)]
    (doseq [[field value] choices]
      (check (or (texts field) (some #{value} (selects field)))
             (str "The " action " form cannot set " field " to " (pr-str value))))
    (request world {:request-method :post :uri "/play"
                    :body (str/join "&" (for [[k v] (concat hidden choices)] (str (encode k) "=" (encode v))))})))

(defn ask-web [world seat question]
  (submit world "ask" [["passenger" seat] ["question" question]]))

(defn- ask-twice [world seat first-seat second-seat]
  (-> world
      (ask-web seat (str "Is " first-seat " an Agent?"))
      (ask-web seat (str "Is " second-seat " an Agent?"))))

(defn accuse [world agent ally]
  (submit world "accuse" [["agent" agent] ["ally" (or ally "")]]))

(defn- check-opening-statements [world]
  (check (= 4 (count (transcript/statements (lines world)))) "Expected four opening statements")
  (transcript/check-reference-statements (lines world))
  world)

(defn- check-questions-left [world n]
  (check (= (parse-long n) (transcript/questions-left (lines world)))
         (str "Questions left: " (transcript/questions-left (lines world))))
  world)

(defn- check-no-roles [world]
  (transcript/check-no-roles (lines world))
  (check (not (re-find #":agent|:awake|:sleeper" (html world))) "The page carries the true roles")
  world)

(defn- check-ask-form [world]
  (let [{:keys [selects texts]} (form-for (html world) "ask")]
    (check (= seat-values (selects "passenger")) "The ask form does not offer every passenger")
    (check (texts "question") "The ask form has no question field"))
  world)

(defn- check-accuse-form [world]
  (let [{:keys [selects]} (form-for (html world) "accuse")]
    (check (= seat-values (selects "agent")) "The accuse form does not offer every passenger")
    (check (= (cons "" seat-values) (selects "ally")) "The ally choice is not optional"))
  world)

(defn- check-answer [world seat expected]
  (check (= [(engine/yes-no expected)] (transcript/answers-of (new-lines world) seat))
         (str "New lines: " (vec (new-lines world))))
  world)

(defn check-see [world text]
  (check (str/includes? (str/join "\n" (new-lines world)) text)
         (str "Did not see " (pr-str text) " in " (vec (new-lines world))))
  world)

(defn check-round [world title]
  (check (nil? (transcript/round-title (new-lines world))) "The round changed")
  (check (= (str/lower-case title) (some-> (transcript/round-title (lines world)) str/lower-case))
         (str "Round is " (transcript/round-title (lines world))))
  world)

(defn check-advanced-round [world title]
  (check (= (str/lower-case title) (some-> (transcript/round-title (new-lines world)) str/lower-case))
         (str "Advanced to " (transcript/round-title (new-lines world))))
  world)

(defn check-score [world score]
  (check (some #{(str "Score: " score)} (new-lines world)) (str "New lines: " (vec (new-lines world))))
  world)

(defn check-over [world]
  (check (some #{"GAME OVER"} (new-lines world)) "GAME OVER not shown")
  (check (empty? (forms (html world))) "The page still accepts moves")
  world)

(defn live-worlds
  "Possible worlds after the moves the page carries."
  [world]
  (let [moves (keep (fn [[k v]] (when (= "move" k) v)) (:hidden (form-for (html world) "ask")))]
    (get-in (web/replay puzzles/reference moves) [:state :live-worlds])))

(def handlers
  [[#"^I (?:open a new|have started a) reference game in the web page$" open-game]
   [#"^I see the four passenger opening statements$" check-opening-statements]
   [#"^I see that (\d+) questions remain$" check-questions-left]
   [#"^I do not see the passengers' true roles$" check-no-roles]
   [#"^I can choose a passenger and enter a yes-or-no question$" check-ask-form]
   [#"^I can accuse a passenger and optionally name an ally$" check-accuse-form]
   [#"^I enter \"(.+)\" as a question for passenger (\S+)$" (fn [world question seat] (ask-web world seat question))]
   [#"^I ask passenger (\S+) \"(.+)\"$" ask-web]
   [#"^I ask passenger (\S+) whether (\S+) and then (\S+) are Agents$" ask-twice]
   [#"^passenger (\S+) answers (\S+)$" check-answer]])

(ns last-train.acceptance.web-steps
  "Step handlers that play the web game through the same session functions
  the page calls (last-train.terminal), and check the built page's HTML."
  (:require [clojure.string :as str]
            [last-train.acceptance.runtime :refer [check]]
            [last-train.acceptance.transcript :as transcript]
            [last-train.site :as site]
            [last-train.terminal :as terminal]))

(def ^:private page-html
  (delay (site/build!)
         (slurp (str site/site-dir "/index.html"))))

(defn- first-draw [_] 0)

(defn- session [world]
  (check (:web world) "No web game is open")
  (:web world))

(defn- shown
  "Transcript text the player sees, without the echoed commands."
  [lines]
  (vec (keep #(when-not (= :player (:kind %)) (:text %)) lines)))

(defn- open [world puzzle]
  (assoc world :web (terminal/boot {:puzzle-param puzzle :rand-int first-draw}) :web-seen 0))

(defn open-game [world] (open world "reference"))

(defn lines [world] (shown (:lines (session world))))

(defn new-lines [world] (shown (drop (:web-seen world) (:lines (session world)))))

(defn- type-command [world text]
  (let [web (session world)]
    (assoc world
           :web (terminal/submit web text first-draw)
           :web-seen (count (:lines web)))))

(defn ask-web [world seat question]
  (type-command world (str "ask " seat " " question)))

(defn accuse [world agent]
  (type-command world (str "accuse " agent)))

(defn- check-opening-statements [world]
  (check (= 4 (count (transcript/statements (lines world)))) "Expected four opening statements")
  (transcript/check-reference-statements (lines world))
  world)

(defn- check-questions-left [world n]
  (transcript/check-questions-left (lines world) n)
  world)

(defn- check-no-roles [world]
  (transcript/check-no-roles (lines world))
  (check (not (re-find #":agent|:human|true-world" @page-html)) "The page carries the true roles")
  world)

(defn- check-prompt [world]
  (check (str/includes? @page-html "id=\"command\"") "The page has no command prompt")
  world)

(defn- check-syntax-shown [world command]
  (check (some #(str/includes? % command) (lines world))
         (str "The page does not show how to type " command))
  world)

(defn- check-type-prompt [world]
  (-> world check-prompt (check-syntax-shown "ask <passenger>") (check-syntax-shown "accuse <passenger>")))

(defn- check-buttons
  "The page has a passenger board, and each passenger's Ask and Accuse
  buttons send commands the game accepts."
  [world]
  (check (str/includes? @page-html "id=\"board\"") "The page has no passenger board")
  (let [{:keys [passengers questions-left over?]} (terminal/controls (session world))]
    (check (= ["A" "B" "C" "D"] (map :seat passengers)) (str "Buttons for " (vec passengers)))
    (check (and (= 3 questions-left) (not over?)) "The buttons start disabled")
    (doseq [{:keys [seat]} passengers]
      (let [asked (terminal/submit (session world) (terminal/ask-command seat "A") first-draw)
            accused (terminal/submit (session world) (terminal/accuse-command seat) first-draw)]
        (check (= 2 (get-in asked [:game :questions-left])) (str "Ask " seat " was not understood"))
        (check (get-in accused [:game :over?]) (str "Accuse " seat " was not understood")))))
  world)

(defn- check-answer [world seat expected]
  (transcript/check-answer (new-lines world) seat expected)
  world)

(defn check-over [world]
  (transcript/check-game-over-shown (new-lines world))
  (check (get-in (session world) [:game :over?]) "The game still accepts moves")
  world)

(defn- check-not-reference [world]
  (check (not= "reference" (get-in (session world) [:game :puzzle :name])) "The reference puzzle started again")
  (check (not-any? #(str/includes? % "Vera (A): \"Tomasz is the Agent.\"") (new-lines world))
         "The reference opening statements are shown again")
  world)

(defn live-worlds
  "Possible worlds in the web game."
  [world]
  (get-in (session world) [:game :live-worlds]))

(def handlers
  [[#"^I (?:open a new|have started a) reference game in the web page$" open-game]
   [#"^I open the web page for the puzzle named \"(.+)\"$" open]
   [#"^I see the four passenger opening statements$" check-opening-statements]
   [#"^I see that (\d+) questions remain$" check-questions-left]
   [#"^I do not see the passengers' true roles$" check-no-roles]
   [#"^I can ask or accuse each passenger with a button$" check-buttons]
   [#"^I can type a question or an accusation at the prompt$" check-type-prompt]
   [#"^I type \"(.+)\" at the prompt$" type-command]
   [#"^I enter \"(.+)\" as a question for passenger (\S+)$" (fn [world question seat] (ask-web world seat question))]
   [#"^I ask passenger (\S+) \"(.+)\"$" ask-web]
   [#"^passenger (\S+) answers (\S+)$" check-answer]
   [#"^a different puzzle starts$" check-not-reference]])

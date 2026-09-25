(ns last-train.acceptance.web-steps
  "Step handlers that play the web game through the same session functions
  the page calls (last-train.terminal), and check the built page's files."
  (:require [clojure.string :as str]
            [last-train.acceptance.runtime :refer [check]]
            [last-train.acceptance.transcript :as transcript]
            [last-train.site :as site]
            [last-train.terminal :as terminal]))

(def ^:private page-html
  (delay (site/build!)
         (slurp (str site/site-dir "/index.html"))))

(def ^:private page-script
  (delay @page-html
         (slurp (str site/site-dir "/terminal.cljs"))))

(defn- first-draw [_] 0)

(defn- session [world]
  (check (:web world) "No web game is open")
  (:web world))

(defn- shown
  "Transcript text the player sees, without the echoed commands."
  [lines]
  (vec (keep #(when-not (= :player (:kind %)) (:text %)) lines)))

(defn- open [world seed]
  (assoc world :web (terminal/boot {:seed-param seed :rand-int first-draw}) :web-seen 0))

(defn lines [world] (shown (:lines (session world))))

(defn new-lines [world] (shown (drop (:web-seen world) (:lines (session world)))))

(defn game-state [world] (:game (session world)))

(defn type-command [world text opts]
  (let [web (session world)]
    (assoc world
           :web (terminal/submit web text first-draw opts)
           :web-seen (count (:lines web)))))

(defn- check-lines [world]
  (let [{:keys [passengers]} (terminal/controls (session world))]
    (check (= 4 (count passengers)) "Expected four passengers")
    (check (every? (comp seq :line) passengers) "A passenger has no line")
    (transcript/check-one-agent (lines world)))
  world)

(defn- check-no-roles [world]
  (transcript/check-no-roles (lines world))
  (check (not (re-find #":agent|:human|true-world" @page-html)) "The page carries the true roles")
  (check (nil? (:last (terminal/controls (session world)))) "The Agent is revealed before an answer")
  world)

(defn- check-tap-and-keys [world]
  (check (str/includes? @page-html "id=\"board\"") "The page has no passenger board")
  (check (str/includes? @page-script "(answer! seat)") "Tapping a passenger does not answer")
  (check (str/includes? @page-script "#{\"a\" \"b\" \"c\" \"d\"}") "Letter keys do not answer")
  world)

(def handlers
  [[#"^I open the web page for seed \"?([^\"]+)\"?$" open]
   [#"^I see the four passenger lines$" check-lines]
   [#"^I do not see the passengers' true roles$" check-no-roles]
   [#"^I can tap each passenger or press their letter$" check-tap-and-keys]
   [#"^I type \"(.+)\" at the prompt$" (fn [world text] (type-command world text {}))]])

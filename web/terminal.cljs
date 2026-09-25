(ns last-train.page
  "DOM glue only: render terminal lines, pass typed and tapped commands on, draw the rain.
  Game rules live in last-train.terminal and below."
  (:require [last-train.terminal :as terminal]))

(defonce session (atom nil))

(defn- el [id] (js/document.getElementById id))

(defn- render! [lines]
  (let [transcript (el "transcript")]
    (doseq [{:keys [text kind]} lines]
      (let [div (js/document.createElement "div")]
        (set! (.-className div) (str "line " (name kind)))
        (set! (.-textContent div) text)
        (.appendChild transcript div)))
    (set! (.-scrollTop transcript) (.-scrollHeight transcript))))

(defn- place-cursor!
  "Put the green block where the next character goes (the font is monospace)."
  [& _]
  (let [input (el "command")
        at (or (.-selectionStart input) (count (.-value input)))]
    (set! (.. (el "cursor") -style -left)
          (str "calc(" at "ch - " (.-scrollLeft input) "px)"))))

(declare ^:private render-controls!)

(defn- play! [text]
  (let [before (count (:lines @session))]
    (swap! session terminal/submit text rand-int)
    (render! (drop before (:lines @session)))
    (render-controls!)))

(defn- submit! [event]
  (.preventDefault event)
  (let [input (el "command")
        text (.-value input)]
    (when-not (= "" (.trim text))
      (play! text))
    (set! (.-value input) "")
    (.focus input)
    (place-cursor!)))

;; What the player is choosing: nil, {:mode :ask :seat "A"} or {:mode :accuse :seat "A"}.
(defonce choice (atom nil))

(defn- node [tag class text]
  (let [n (js/document.createElement tag)]
    (when class (set! (.-className n) class))
    (when text (set! (.-textContent n) text))
    n))

(defn- button [class text on-click]
  (let [b (node "button" class text)]
    (set! (.-type b) "button")
    (.addEventListener b "click" (fn [_] (on-click)))
    b))

(defn- choose! [c]
  (reset! choice c)
  (render-controls!))

(defn- act! [text]
  (reset! choice nil)
  (play! text))

(defn- card [{:keys [seat name bio]} {:keys [questions-left over?]}]
  (let [c (node "div" "card" nil)
        actions (node "div" "actions" nil)
        ask (button "ask" "Ask" #(choose! {:mode :ask :seat seat}))]
    (.appendChild c (doto (node "div" "who" nil)
                      (.appendChild (node "b" nil seat))
                      (.appendChild (js/document.createTextNode (str " " name)))))
    (.appendChild c (node "div" "bio" bio))
    (set! (.-disabled ask) (or over? (zero? questions-left)))
    (.appendChild actions ask)
    (.appendChild actions (button "accuse" "Accuse" #(choose! {:mode :accuse :seat seat})))
    (when over? (set! (.-disabled (.-lastChild actions)) true))
    (.appendChild c actions)
    (when (= seat (:seat @choice)) (.add (.-classList c) "chosen"))
    c))

(defn- picker [{:keys [passengers over?]}]
  (let [p (node "div" "pick" nil)
        {:keys [mode seat]} @choice
        name-of (fn [s] (:name (first (filter #(= s (:seat %)) passengers))))]
    (cond
      over?
      (.appendChild p (button "go" "Play again" #(act! "new")))

      (= :ask mode)
      (do (.appendChild p (node "span" "prompt-text" (str "Ask " (name-of seat) ": is ... the Agent?")))
          (doseq [{target :seat target-name :name} passengers]
            (.appendChild p (button "go" (if (= target seat) (str target-name " (self)") target-name)
                                    #(act! (terminal/ask-command seat target)))))
          (.appendChild p (button "cancel" "Cancel" #(choose! nil))))

      (= :accuse mode)
      (do (.appendChild p (node "span" "prompt-text" (str "Accuse " (name-of seat) "? This ends the game.")))
          (.appendChild p (button "go danger" "Pull the brake" #(act! (terminal/accuse-command seat))))
          (.appendChild p (button "cancel" "Cancel" #(choose! nil))))

      :else
      (.appendChild p (node "span" "prompt-text" "Tap Ask to question a passenger, or Accuse to name the Agent.")))
    p))

(defn- render-controls! []
  (let [controls (terminal/controls @session)
        board (el "board")
        pick (el "picker")]
    (set! (.-innerHTML board) "")
    (doseq [passenger (:passengers controls)]
      (.appendChild board (card passenger controls)))
    (set! (.-innerHTML pick) "")
    (.appendChild pick (picker controls))))

(def ^:private glyphs "ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐﾑﾒﾓﾔﾕﾖﾗﾘﾙﾚﾛﾜﾝ0123456789")

(defn- start-rain! []
  (let [canvas (el "rain")
        ctx (.getContext canvas "2d")
        size 16
        drops (atom [])
        resize! (fn [& _]
                  (set! (.-width canvas) js/innerWidth)
                  (set! (.-height canvas) js/innerHeight)
                  (reset! drops (vec (repeatedly (js/Math.ceil (/ js/innerWidth size))
                                                 #(rand-int 50)))))
        last-frame (atom 0)
        frame (fn frame [t]
                (when (> (- t @last-frame) 50)
                  (reset! last-frame t)
                  (set! (.-fillStyle ctx) "rgba(0,0,0,0.08)")
                  (.fillRect ctx 0 0 (.-width canvas) (.-height canvas))
                  (set! (.-fillStyle ctx) "#33ff66")
                  (set! (.-font ctx) (str size "px monospace"))
                  (swap! drops
                         (fn [ds]
                           (vec (map-indexed
                                  (fn [i y]
                                    (.fillText ctx (nth glyphs (rand-int (count glyphs))) (* i size) (* y size))
                                    (if (and (> (* y size) (.-height canvas)) (> (rand) 0.975)) 0 (inc y)))
                                  ds)))))
                (js/requestAnimationFrame frame))]
    (resize!)
    (.addEventListener js/window "resize" resize!)
    (js/requestAnimationFrame frame)))

;; Any printable key goes to the prompt, so the player never has to click it first.
;; Space and Enter on a focused button still press that button.
(defn- type-anywhere! [event]
  (let [input (el "command")
        active js/document.activeElement
        k (.-key event)]
    (when (and (= 1 (count k))
               (not (or (.-ctrlKey event) (.-metaKey event) (.-altKey event)))
               (not= input active)
               (not (and (= " " k) (= "BUTTON" (.-tagName active)))))
      (.preventDefault event)
      (set! (.-value input) (str (.-value input) k))
      (.focus input)
      (.setSelectionRange input (count (.-value input)) (count (.-value input)))
      (js/requestAnimationFrame place-cursor!))))

;; Scittle checks arity: every event handler must accept the event argument.
(defn- show-rules! [open?]
  (set! (.-hidden (el "rules")) (not open?))
  (.setAttribute (el "rules-toggle") "aria-expanded" (str open?)))

(defn- start-rules! []
  (.addEventListener (el "rules-toggle") "click"
                     (fn [_] (show-rules! (.-hidden (el "rules")))))
  (.addEventListener js/document "keydown"
                     #(when (= "Escape" (.-key %)) (show-rules! false))))

(defn- init! []
  (let [params (js/URLSearchParams. (.-search js/location))]
    (reset! session (terminal/boot {:puzzle-param (.get params "puzzle") :rand-int rand-int}))
    (render! (:lines @session))
    (render-controls!)
    (.addEventListener (el "prompt") "submit" submit!)
    (start-rules!)
    (doseq [event ["input" "keydown" "keyup" "click" "focus" "select" "scroll"]]
      (.addEventListener (el "command") event (fn [_] (js/requestAnimationFrame place-cursor!))))
    (.addEventListener js/document "selectionchange" (fn [_] (js/requestAnimationFrame place-cursor!)))
    (.addEventListener js/document "keydown" type-anywhere!)
    (place-cursor!)
    (.focus (el "command"))
    (set! (.. js/document -body -dataset -ready) "true")
    (when-not (.-matches (js/matchMedia "(prefers-reduced-motion: reduce)"))
      (start-rain!))))

(init!)

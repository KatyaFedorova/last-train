(ns last-train.page
  "DOM glue only: draw the passengers, run the clock, pass keys and taps on.
  Game rules live in last-train.terminal and below."
  (:require [last-train.terminal :as terminal]))

(defonce session (atom nil))

;; :playing (clock running) or :result (showing the last answer, waiting for Next).
(defonce phase (atom :playing))
;; Controls of the train just answered, so the result shows its cards.
(defonce answered (atom nil))
;; Latest outcome and operator lines to show under the cards.
(defonce message (atom []))
(defonce deadline (atom 0))

(defn- el [id] (js/document.getElementById id))

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

(declare ^:private render!)

(defn- seconds-left []
  (max 0 (js/Math.ceil (/ (- @deadline (js/Date.now)) 1000))))

(defn- run-command!
  "Submit text; returns the new transcript lines that are not echoes."
  [text opts]
  (let [before (count (:lines @session))]
    (swap! session terminal/submit text rand-int opts)
    (->> (:lines @session) (drop before) (remove #(= :player (:kind %))))))

(defn- start-clock! []
  (reset! deadline (+ (js/Date.now) (* 1000 (:seconds (terminal/controls @session))))))

(defn- answer! [text]
  (when (= :playing @phase)
    (let [before (terminal/controls @session)
          seconds (seconds-left)
          lines (run-command! text {:seconds-left seconds})]
      (when (:last (terminal/controls @session))
        (reset! answered before)
        (reset! phase :result)
        (reset! message (filter #(#{:outcome :operator} (:kind %)) lines)))
      (render!))))

(defn- hint! []
  (when (= :playing @phase)
    (reset! message (run-command! "hint" {}))
    (render!)))

(defn- next! []
  (when (= :result @phase)
    (when (:over? (terminal/controls @session))
      (run-command! "new" {}))
    (.blur js/document.activeElement)
    (reset! phase :playing)
    (reset! answered nil)
    (reset! message [])
    (start-clock!)
    (render!)))

(defn- card [{:keys [seat name bio line]} marks]
  (let [c (button (str "card " (marks seat)) nil #(answer! seat))]
    (.appendChild c (node "span" "letter" seat))
    (.appendChild c (node "span" "who" name))
    (.appendChild c (node "span" "bio" bio))
    (.appendChild c (node "span" "quote" (str "\u201c" line "\u201d")))
    (.setAttribute c "aria-label" (str seat ", " name ": " line))
    c))

(defn- render! []
  (let [now (terminal/controls @session)
        shown (or @answered now)
        {:keys [agent guess right?]} (:last now)
        marks (if @answered
                (cond-> {agent "agent"} (and guess (not right?)) (assoc guess "wrong"))
                {})
        board (el "board")
        result (el "result")]
    (set! (.-textContent (el "status"))
          (str "Train " (:train shown) " of " (:trains now) "  \u00b7  Score " (:score now)))
    (set! (.-innerHTML board) "")
    (doseq [p (:passengers shown)]
      (.appendChild board (card p marks)))
    (.toggle (.-classList board) "answered" (boolean @answered))
    (set! (.-innerHTML result) "")
    (doseq [{:keys [text kind]} @message]
      (.appendChild result (node "p" (str "line " (name kind)) text)))
    (let [actions (node "div" "actions" nil)]
      (if (= :result @phase)
        (.appendChild actions (button "go" (if (:over? now) "Play again (Enter)" "Next train (Enter)") next!))
        (do (.appendChild result (node "p" "line system" "Who is the Agent? Tap a passenger or press A, B, C or D."))
            (when (:hint? now) (.appendChild actions (button "cancel" "Tip (H)" hint!)))))
      (.appendChild result actions))))

(defn- tick! [_]
  (let [fill (el "timer-fill")
        total (* 1000 (:seconds (terminal/controls @session)))
        left (max 0 (- @deadline (js/Date.now)))]
    (when (= :playing @phase)
      (set! (.. fill -style -width) (str (* 100 (/ left total)) "%"))
      (.toggle (.-classList fill) "low" (< left 5000))
      (when (zero? left) (answer! "time"))))
  (js/requestAnimationFrame tick!))

(defn- on-key! [event]
  (let [k (.toLowerCase (.-key event))]
    (when-not (or (.-ctrlKey event) (.-metaKey event) (.-altKey event))
      (cond
        (and (#{"a" "b" "c" "d"} k) (= :playing @phase)) (do (.preventDefault event) (answer! (.toUpperCase k)))
        (and (= "h" k) (= :playing @phase)) (hint!)
        (and (#{"enter" " " "n"} k) (= :result @phase)) (do (.preventDefault event) (next!))))))

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
    (reset! session (terminal/boot {:seed-param (.get params "seed") :rand-int rand-int}))
    (start-rules!)
    (.addEventListener js/document "keydown" on-key!)
    (start-clock!)
    (render!)
    (js/requestAnimationFrame tick!)
    (set! (.. js/document -body -dataset -ready) "true")
    (when-not (.-matches (js/matchMedia "(prefers-reduced-motion: reduce)"))
      (start-rain!))))

(init!)

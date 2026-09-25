(ns last-train.page
  "DOM glue only: render terminal lines, pass typed commands on, draw the rain.
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

(defn- submit! [event]
  (.preventDefault event)
  (let [input (el "command")
        text (.-value input)
        before (count (:lines @session))]
    (when-not (= "" (.trim text))
      (swap! session terminal/submit text rand-int)
      (render! (drop before (:lines @session))))
    (set! (.-value input) "")
    (.focus input)
    (place-cursor!)))

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
    (reset! session (terminal/boot {:puzzle-param (.get params "puzzle") :rand-int rand-int}))
    (render! (:lines @session))
    (.addEventListener (el "prompt") "submit" submit!)
    (start-rules!)
    (doseq [event ["input" "keydown" "keyup" "click" "focus" "select" "scroll"]]
      (.addEventListener (el "command") event (fn [_] (js/requestAnimationFrame place-cursor!))))
    (.addEventListener js/document "selectionchange" (fn [_] (js/requestAnimationFrame place-cursor!)))
    (place-cursor!)
    (.focus (el "command"))
    (set! (.. js/document -body -dataset -ready) "true")
    (when-not (.-matches (js/matchMedia "(prefers-reduced-motion: reduce)"))
      (start-rain!))))

(init!)

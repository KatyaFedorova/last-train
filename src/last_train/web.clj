(ns last-train.web
  "Browser game: pure request -> response. The page carries every move in
  hidden fields, so each request replays the deterministic game from the start."
  (:require [clojure.string :as str]
            [last-train.game :as game]
            [last-train.logic :as logic])
  (:import (java.net URLDecoder)))

(defn- decode [text]
  (URLDecoder/decode text "UTF-8"))

(defn parse-form
  "Url-encoded body as {field [values in order]}."
  [body]
  (reduce (fn [fields pair]
            (let [[k v] (str/split pair #"=" 2)]
              (update fields (decode k) (fnil conj []) (decode (or v "")))))
          {}
          (remove str/blank? (str/split (or body "") #"&"))))

(defn- field [params k]
  (str/trim (first (get params k [""]))))

(defn input-line
  "Game input for a submitted ask or accuse form, or nil."
  [params]
  (case (field params "action")
    "ask" (str "ask " (field params "passenger") " " (str/replace (field params "question") #"\s+" " "))
    "accuse" (let [ally (field params "ally")]
               (str "accuse " (field params "agent") (when (seq ally) (str " ally " ally))))
    nil))

(defn replay
  "Game state and full transcript after playing moves from the start."
  [puzzle moves]
  (reduce (fn [{:keys [state output]} move]
            (let [step (game/handle state move)]
              {:state (:state step) :output (into output (:output step))}))
          (update (game/start puzzle) :output vec)
          moves))

(defn- escape [text]
  (str/escape (str text) {\& "&amp;" \< "&lt;" \> "&gt;" \" "&quot;" \' "&#39;"}))

(defn- hidden [name value]
  (str "<input type=\"hidden\" name=\"" name "\" value=\"" (escape value) "\">"))

(defn- passenger-select [name puzzle blank]
  (str "<select name=\"" name "\">"
       (when blank (str "<option value=\"\">" blank "</option>"))
       (str/join (for [seat logic/seats]
                   (str "<option value=\"" (clojure.core/name seat) "\">" (clojure.core/name seat)
                        " - " (escape (get-in puzzle [:personas seat :name])) "</option>")))
       "</select>"))

(defn- play-form [moves action fields button]
  (str "<form method=\"post\" action=\"/play\">"
       (str/join (map #(hidden "move" %) moves))
       (hidden "action" action)
       fields
       "<button type=\"submit\">" button "</button></form>"))

(defn- controls [puzzle moves]
  (str (play-form moves "ask"
                  (str "<label>Passenger " (passenger-select "passenger" puzzle nil) "</label> "
                       "<label>Question <input type=\"text\" name=\"question\" placeholder=\"Is D an Agent?\"></label> ")
                  "Ask")
       (play-form moves "accuse"
                  (str "<label>Accuse " (passenger-select "agent" puzzle nil) "</label> "
                       "<label>Awake ally " (passenger-select "ally" puzzle "no ally") "</label> ")
                  "Accuse")))

(def ^:private style
  "body{background:#000;color:#3f3;font-family:monospace;margin:0 auto;max-width:48rem;padding:1rem}
pre{white-space:pre-wrap}form{margin:1rem 0}a,button,input,select{background:#000;color:#3f3;border:1px solid #3f3;font:inherit}")

(defn page
  "HTML page for the game after moves."
  [puzzle moves]
  (let [{:keys [state output]} (replay puzzle moves)]
    (str "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\">"
         "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
         "<title>LAST TRAIN</title><style>" style "</style></head><body>"
         "<pre id=\"transcript\">" (escape (str/join "\n" output)) "</pre>"
         (if (:over? state)
           "<p><a href=\"/\">New game</a></p>"
           (controls puzzle moves))
         "</body></html>")))

(defn- html [body]
  {:status 200 :headers {"Content-Type" "text/html; charset=utf-8"} :body body})

(defn respond
  "Response for a request {:request-method :uri :body}."
  [puzzle {:keys [request-method uri body]}]
  (case [request-method uri]
    [:get "/"] (html (page puzzle []))
    [:post "/play"] (let [params (parse-form body)
                          moves (get params "move" [])]
                      (html (page puzzle (if-let [line (input-line params)] (conj moves line) moves))))
    {:status 404 :headers {"Content-Type" "text/plain; charset=utf-8"} :body "Not found"}))

(defn parse-args
  "Server options: {:port n} or {:error message}."
  [args]
  (let [[option value & more] (if-let [[_ v] (some->> (first args) (re-matches #"--port=(.*)"))]
                                (list* "--port" v (rest args))
                                args)
        port (some-> value parse-long)]
    (cond
      (empty? args) {:port 8080}
      (not= "--port" option) {:error (str "Unknown option: " option)}
      (nil? value) {:error "Missing value for --port"}
      (not (and port (< 0 port 65536))) {:error (str "Bad port: " value)}
      (seq more) {:error (str "Unknown option: " (first more))}
      :else {:port port})))

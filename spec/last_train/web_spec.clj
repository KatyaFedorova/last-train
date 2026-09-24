(ns last-train.web-spec
  (:require [speclj.core :refer :all]
            [clojure.string :as str]
            [last-train.puzzles :as puzzles]
            [last-train.web :as web]))

(defn- get-page [uri]
  (web/respond puzzles/reference {:request-method :get :uri uri}))

(defn- post-play [body]
  (web/respond puzzles/reference {:request-method :post :uri "/play" :body body}))

(describe "Form bodies"
  (it "decodes url-encoded fields and keeps repeated fields in order"
    (should= {"move" ["ask B Is D an Agent?" "accuse A"] "action" ["ask"]}
             (web/parse-form "move=ask+B+Is+D+an+Agent%3F&action=ask&move=accuse%20A")))

  (it "reads a missing or empty body as no fields"
    (should= {} (web/parse-form nil))
    (should= {} (web/parse-form "")))

  (it "reads a field without a value as empty"
    (should= {"ally" [""]} (web/parse-form "ally"))))

(describe "Moves from the page's forms"
  (it "turns the ask form into an ask line"
    (should= "ask B Is D an Agent?"
             (web/input-line {"action" ["ask"] "passenger" ["B"] "question" ["  Is D an Agent?\n"]})))

  (it "turns the accuse form into an accuse line with an optional ally"
    (should= "accuse A ally D" (web/input-line {"action" ["accuse"] "agent" ["A"] "ally" ["D"]}))
    (should= "accuse A" (web/input-line {"action" ["accuse"] "agent" ["A"] "ally" [""]}))
    (should= "accuse A" (web/input-line {"action" ["accuse"] "agent" ["A"]})))

  (it "ignores a submission without a known action"
    (should-be-nil (web/input-line {}))
    (should-be-nil (web/input-line {"action" ["dance"]}))))

(describe "Replaying moves"
  (it "starts the game when there are no moves"
    (let [{:keys [state output]} (web/replay puzzles/reference [])]
      (should= 3 (:questions-left state))
      (should= "LAST TRAIN" (first output))))

  (it "applies each move in order and keeps the whole transcript"
    (let [{:keys [state output]} (web/replay puzzles/reference ["ask B Is D an Agent?" "accuse A ally D"])]
      (should (:over? state))
      (should-contain "Tomasz (B): \"No. Mr. Grey is not an Agent.\"" output)
      (should= "GAME OVER" (last output)))))

(describe "The game page"
  (it "starts a new reference game on the home page"
    (let [{:keys [status headers body]} (get-page "/")]
      (should= 200 status)
      (should= "text/html; charset=utf-8" (get headers "Content-Type"))
      (should-contain "<title>LAST TRAIN</title>" body)
      (should-contain "Vera (A): &quot;There are no Agents on this train.&quot;" body)
      (should-contain "Questions left: 3" body)))

  (it "offers an ask form and an accuse form naming every passenger"
    (let [body (:body (get-page "/"))]
      (should-contain "<input type=\"hidden\" name=\"action\" value=\"ask\">" body)
      (should-contain "<input type=\"text\" name=\"question\"" body)
      (should-contain "<input type=\"hidden\" name=\"action\" value=\"accuse\">" body)
      (should-contain "<select name=\"ally\"><option value=\"\">no ally</option>" body)
      (should-contain "<option value=\"D\">D - Mr. Grey</option>" body)
      (doseq [field ["passenger" "agent"]]
        (should-contain (str "<select name=\"" field "\"><option value=\"A\">A - Vera</option>") body))))

  (it "does not reveal the true roles"
    (let [body (:body (get-page "/"))]
      (should-not (re-find #":agent|:awake|:sleeper|AGENT|AWAKE|SLEEPER" body))))

  (it "plays a submitted question and carries the moves in hidden fields"
    (let [body (:body (post-play "action=ask&passenger=B&question=Is+D+an+Agent%3F"))]
      (should-contain "Tomasz (B): &quot;No. Mr. Grey is not an Agent.&quot;" body)
      (should-contain "Questions left: 2" body)
      (should-contain "<input type=\"hidden\" name=\"move\" value=\"ask B Is D an Agent?\">" body)))

  (it "replays earlier moves before the new one"
    (let [body (:body (post-play "move=ask+B+Is+D+an+Agent%3F&action=accuse&agent=A&ally=D"))]
      (should-contain "PERFECT RUN" body)
      (should-contain "Score: 200" body)))

  (it "escapes player text in the transcript and hidden fields"
    (let [body (:body (post-play "move=ask+B+%3Cb%3E%26%27&action=ask&passenger=B&question=x"))]
      (should-contain "value=\"ask B &lt;b&gt;&amp;&#39;\"" body)
      (should-not-contain "<b>&'" body)))

  (it "ignores a submission without an action"
    (should= (:body (get-page "/")) (:body (post-play ""))))

  (it "replaces the forms with a new game link when the game is over"
    (let [body (:body (post-play "action=accuse&agent=D&ally="))]
      (should-contain "LOSE" body)
      (should-contain "<a href=\"/\">New game</a>" body)
      (should-not-contain "<form" body)))

  (it "reports unknown pages as not found"
    (should= 404 (:status (get-page "/nowhere")))
    (should= 404 (:status (web/respond puzzles/reference {:request-method :get :uri "/play"})))))

(describe "Server options"
  (it "defaults to port 8080"
    (should= {:port 8080} (web/parse-args [])))

  (it "reads the port option"
    (should= {:port 9000} (web/parse-args ["--port" "9000"]))
    (should= {:port 9000} (web/parse-args ["--port=9000"])))

  (it "reports a bad port or unknown option"
    (should-contain :error (web/parse-args ["--port" "x"]))
    (should-contain :error (web/parse-args ["--port"]))
    (should-contain :error (web/parse-args ["--port" "0"]))
    (should-contain :error (web/parse-args ["--port" "65536"]))
    (should-contain :error (web/parse-args ["--port" "9000" "extra"]))
    (should-contain :error (web/parse-args ["--seed" "reference"]))))

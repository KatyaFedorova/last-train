(ns last-train.acceptance.runner-worker-spec
  (:require [speclj.core :refer :all]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [last-train.acceptance.runner-worker :as worker]))

(def steps
  [[#"^the total is (\S+)$" (fn [world n] (last-train.acceptance.runtime/check (= "3" n) "not three") world)]])

(defn- ir-file [total]
  (let [file (io/file "tmp/runner-worker-spec" (str "feature-" total ".json"))]
    (io/make-parents file)
    (spit file (json/generate-string
                 {"name" "Totals"
                  "scenarios" [{"name" "total" "steps" [{"keyword" "Then" "text" "the total is <t>"}]
                                "examples" [{"t" total}]}]}))
    (str file)))

(defn- job [path] {"id" "m1" "feature_json" path "generated_dir" "g" "work_dir" "w" "timeout" "30s"})

(describe "Runner worker jobs"
  (it "reports success when every execution passes"
    (let [response (worker/run-job steps (job (ir-file "3")))]
      (should= "m1" (response "id"))
      (should= "test_success" (response "outcome"))
      (should= "" (response "error"))
      (should (integer? (response "duration")))))

  (it "reports failure when an execution fails"
    (let [response (worker/run-job steps (job (ir-file "4")))]
      (should= "test_failure" (response "outcome"))
      (should-contain "not three" (response "output"))))

  (it "reports an infrastructure error when the IR cannot be read"
    (let [response (worker/run-job steps (job "tmp/runner-worker-spec/missing.json"))]
      (should= "infrastructure_error" (response "outcome"))
      (should-not (str/blank? (response "error"))))))

(describe "Runner worker protocol"
  (it "answers one JSON line per JSON job line"
    (let [input (str (json/generate-string (job (ir-file "3"))) "\n"
                     (json/generate-string (assoc (job (ir-file "4")) "id" "m2")) "\n")
          out (with-out-str (with-in-str input (worker/serve steps)))
          responses (map json/parse-string (str/split-lines out))]
      (should= [["m1" "test_success"] ["m2" "test_failure"]]
               (map (juxt #(% "id") #(% "outcome")) responses))))

  (it "answers a malformed job line with an infrastructure error"
    (let [out (with-out-str (with-in-str "not json\n" (worker/serve steps)))]
      (should= "infrastructure_error" ((json/parse-string out) "outcome")))))

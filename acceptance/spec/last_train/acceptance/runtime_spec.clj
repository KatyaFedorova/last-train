(ns last-train.acceptance.runtime-spec
  (:require [speclj.core :refer :all]
            [last-train.acceptance.runtime :as runtime]))

(def steps
  [[#"^the total starts at (\S+)$" (fn [world n] (assoc world :total (parse-long n)))]
   [#"^I add (\S+)$" (fn [world n] (update world :total + (parse-long n)))]
   [#"^the total is (\S+)$" (fn [world n] (runtime/check (= (parse-long n) (:total world))
                                                         (str "total was " (:total world)))
                              world)]
   [#"^I add twice$" (fn [world] world)]
   [#"^I add (?:twice|thrice)$" (fn [world] world)]])

(def ir
  {"name" "Adding"
   "background" [{"keyword" "Given" "text" "the total starts at 1"}]
   "scenarios" [{"name" "plain"
                 "steps" [{"keyword" "When" "text" "I add 2"}
                          {"keyword" "Then" "text" "the total is 3"}]
                 "examples" []}
                {"name" "outline"
                 "steps" [{"keyword" "When" "text" "I add <n>"}
                          {"keyword" "Then" "text" "the total is <total>"}]
                 "examples" [{"n" "1" "total" "2"} {"n" "5" "total" "6"} {"n" "5" "total" "9"}]}]})

(describe "Scenario executions"
  (it "runs a scenario without examples once and each example row once"
    (should= [{} {"n" "1" "total" "2"} {"n" "5" "total" "6"} {"n" "5" "total" "9"}]
             (map :example (runtime/executions ir))))

  (it "prepends background steps to every execution"
    (should= ["the total starts at 1" "I add <n>" "the total is <total>"]
             (map #(get % "text") (:steps (second (runtime/executions ir))))))

  (it "names executions by scenario and one-based example index"
    (should= ["plain/example_1" "outline/example_1" "outline/example_2" "outline/example_3"]
             (map :name (runtime/executions ir)))))

(describe "Running executions"
  (it "passes when every step succeeds, background first"
    (should-be-nil (runtime/run-execution steps ir 0 0))
    (should-be-nil (runtime/run-execution steps ir 1 0))
    (should-be-nil (runtime/run-execution steps ir 1 1)))

  (it "reports a failed assertion"
    (should-contain "total was 6" (runtime/run-execution steps ir 1 2)))

  (it "reports unsupported steps"
    (should-contain "Unsupported step: I subtract 1"
                    (runtime/run-execution steps (assoc-in ir ["scenarios" 0 "steps" 0 "text"] "I subtract 1") 0 0)))

  (it "reports ambiguous steps"
    (should-contain "Ambiguous step: I add twice"
                    (runtime/run-execution steps (assoc-in ir ["scenarios" 0 "steps" 0 "text"] "I add twice") 0 0)))

  (it "reports missing example values"
    (should-contain "Missing example value: n"
                    (runtime/run-execution steps (assoc-in ir ["scenarios" 1 "examples" 0] {"total" "2"}) 1 0)))

  (it "reports handler errors such as malformed values"
    (should-contain "Error in step: I add x"
                    (runtime/run-execution steps (assoc-in ir ["scenarios" 0 "steps" 0 "text"] "I add x") 0 0))))

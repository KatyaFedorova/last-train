(ns last-train.cli-spec
  (:require [speclj.core :refer :all]
            [clojure.string :as str]
            [last-train.cli :as cli]
            [last-train.puzzles :as puzzles]))

(describe "Command line options"
  (it "loads the reference puzzle with template voices"
    (should= {:puzzle puzzles/reference :voices "template"}
             (cli/parse-args ["--seed" "reference" "--voices=template"]))
    (should= {:puzzle puzzles/reference :voices "template"}
             (cli/parse-args ["--seed=reference" "--voices" "template"])))

  (it "defaults to the reference puzzle and template voices"
    (should= {:puzzle puzzles/reference :voices "template"} (cli/parse-args [])))

  (it "reports unsupported seeds, voices and options"
    (should-contain :error (cli/parse-args ["--seed" "42"]))
    (should-contain :error (cli/parse-args ["--voices=llm"]))
    (should-contain :error (cli/parse-args ["--seed"]))
    (should-contain :error (cli/parse-args ["--debug"]))))

(describe "Terminal play"
  (it "prompts for each line and prints the game's replies"
    (let [out (with-out-str (with-in-str "ask B is D an Agent?\naccuse A ally D\n"
                              (cli/play puzzles/reference)))]
      (should-contain "LAST TRAIN\n" out)
      (should-contain "> Tomasz (B): \"No. Mr. Grey is not an Agent.\"\n" out)
      (should-contain "PERFECT RUN" out)
      (should (str/ends-with? out "GAME OVER\n"))))

  (it "stops when input runs out"
    (let [out (with-out-str (with-in-str "" (cli/play puzzles/reference)))]
      (should (str/ends-with? out "> ")))))

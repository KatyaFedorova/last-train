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

  (it "loads any catalog puzzle by name"
    (doseq [{:keys [name] :as puzzle} puzzles/catalog]
      (should= {:puzzle puzzle :voices "template"} (cli/parse-args ["--seed" name]))))

  (it "picks a random puzzle for the random seed"
    (should= {:puzzle (puzzles/pick (constantly 2) nil) :voices "template"}
             (cli/parse-args ["--seed" "random"] (constantly 2))))

  (it "names every supported seed when the seed is unknown"
    (should= {:error "Unknown seed: 42. Supported: reference, commuters, night-shift, terminus, red-eye, random"}
             (cli/parse-args ["--seed" "42"])))

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

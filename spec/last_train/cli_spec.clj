(ns last-train.cli-spec
  (:require [speclj.core :refer :all]
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

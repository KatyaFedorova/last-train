(ns last-train.cli-spec
  (:require [speclj.core :refer :all]
            [clojure.string :as str]
            [last-train.cli :as cli]
            [last-train.game :as game]))

(describe "Command line options"
  (it "takes a numeric seed"
    (should= {:seed 42} (cli/parse-args ["--seed" "42"]))
    (should= {:seed 7} (cli/parse-args ["--seed=7"])))

  (it "picks a random seed by default or for random"
    (should= {:seed 3} (cli/parse-args [] (constantly 3)))
    (should= {:seed 3} (cli/parse-args ["--seed" "random"] (constantly 3))))

  (it "reports bad seeds and options"
    (should= {:error "Unknown seed: reference. Use a number or random."} (cli/parse-args ["--seed" "reference"]))
    (should-contain :error (cli/parse-args ["--seed"]))
    (should-contain :error (cli/parse-args ["--debug"]))))

(describe "Terminal play"
  (it "prompts for each line and prints the game's replies"
    (let [out (with-out-str (with-in-str (str/join "\n" (repeat 10 "time"))
                              (cli/play 42)))]
      (should-contain "LAST TRAIN\n" out)
      (should-contain (str (first (:output (game/start 42))) "\n") out)
      (should-contain "> Time's up! " out)
      (should (str/ends-with? out "GAME OVER\n"))))

  (it "stops when input runs out"
    (let [out (with-out-str (with-in-str "" (cli/play 42)))]
      (should (str/ends-with? out "> ")))))

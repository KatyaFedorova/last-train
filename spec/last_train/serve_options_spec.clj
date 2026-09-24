(ns last-train.serve-options-spec
  (:require [speclj.core :refer :all]
            [last-train.serve-options :as options]))

(describe "Local server options"
  (it "serves on port 8080 by default"
    (should= {:port 8080} (options/parse-args [])))

  (it "reads the port option"
    (should= {:port 9000} (options/parse-args ["--port" "9000"]))
    (should= {:port 9000} (options/parse-args ["--port=9000"])))

  (it "reports a bad port or unknown option"
    (should= {:error "Bad port: x"} (options/parse-args ["--port" "x"]))
    (should= {:error "Missing value for --port"} (options/parse-args ["--port"]))
    (should= {:error "Bad port: 0"} (options/parse-args ["--port" "0"]))
    (should= {:error "Bad port: 65536"} (options/parse-args ["--port" "65536"]))
    (should= {:error "Unknown option: extra"} (options/parse-args ["--port" "9000" "extra"]))
    (should= {:error "Unknown option: --seed"} (options/parse-args ["--seed" "reference"]))))

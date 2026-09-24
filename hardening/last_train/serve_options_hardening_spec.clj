(ns last-train.serve-options-hardening-spec
  (:require [speclj.core :refer :all]
            [last-train.serve-options :as options]))

(describe "Local server option hardening"
  (it "reports the first unexpected argument after a valid port"
    (should= {:error "Unknown option: extra"}
             (options/parse-args ["--port" "9000" "extra"]))))

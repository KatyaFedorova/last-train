(ns last-train.web-hardening-spec
  (:require [speclj.core :refer :all]
            [last-train.web :as web]))

(describe "Web server option hardening"
  (it "reports the first unexpected argument after a valid port"
    (should= {:error "Unknown option: extra"}
             (web/parse-args ["--port" "9000" "extra"]))))

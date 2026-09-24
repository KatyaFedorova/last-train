(ns last-train.acceptance.bin-acceptance-spec
  (:require [speclj.core :refer :all]
            [babashka.process :as process]
            [clojure.string :as str]))

(describe "bin/acceptance without a Gherkin parser"
  (it "stops and names both ways to provide one"
    (let [{:keys [exit err]} @(process/process ["bin/acceptance"]
                                                {:out :string :err :string
                                                 :extra-env {"APS_HOME" "/nonexistent/aps"
                                                             "PATH" "/usr/bin:/bin"}})]
      (should= 1 exit)
      (should (str/includes? err "gherkin-parser"))
      (should (str/includes? err "APS_HOME")))))

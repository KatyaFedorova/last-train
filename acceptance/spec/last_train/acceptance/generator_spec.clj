(ns last-train.acceptance.generator-spec
  (:require [speclj.core :refer :all]
            [clojure.string :as str]
            [cheshire.core :as json]
            [last-train.acceptance.generator :as generator]))

(def ir-json
  (json/generate-string
    {"name" "CLI start"
     "scenarios" [{"name" "one" "steps" [{"keyword" "When" "text" "x"}] "examples" []}
                  {"name" "two" "steps" [{"keyword" "When" "text" "x <a>"}]
                   "examples" [{"a" "1"} {"a" "2"}]}]}))

(def output (generator/generate "build/acceptance/ir/cli-start.json" ir-json "acceptance/generated"))
(def test-path "acceptance/generated/last_train/generated/cli_start_spec.clj")
(def metadata-path "acceptance/generated/metadata/features-cli-start-feature.json")

(describe "Metadata names"
  (it "lowercases and hyphenates the feature path"
    (should= "features-hunt-the-wumpus-feature.json" (generator/metadata-name "features/Hunt The Wumpus.feature"))
    (should= "features-api-v2-happy-path-feature.json" (generator/metadata-name "Features/API v2/Happy Path.feature"))))

(describe "Generated entry points"
  (it "writes one test file and one metadata file"
    (should= #{test-path metadata-path} (set (keys output))))

  (it "embeds the IR and runs every scenario execution through the runtime"
    (let [source (output test-path)]
      (should-contain "(ns last-train.generated.cli-start-spec" source)
      (should-contain (pr-str ir-json) source)
      (should-contain "\"one/example_1\"" source)
      (should-contain "\"two/example_1\"" source)
      (should-contain "\"two/example_2\"" source)
      (should-contain "(runtime/run-execution steps/handlers ir 1 1)" source)
      (should-not-contain "two/example_3" source)))

  (it "is deterministic"
    (should= output (generator/generate "build/acceptance/ir/cli-start.json" ir-json "acceptance/generated")))

  (it "describes the feature in metadata with a hash of the generated file only"
    (let [metadata (json/parse-string (output metadata-path))]
      (should= 1 (metadata "schema_version"))
      (should= "features/cli-start.feature" (metadata "feature_path"))
      (should= "build/acceptance/ir/cli-start.json" (metadata "ir_path"))
      (should= "generated_files" (metadata "hash_scope"))
      (should= [test-path] (metadata "generated_files"))
      (should (str/starts-with? (metadata "implementation_hash") "sha256:"))
      (should-not= (metadata "implementation_hash")
                   ((json/parse-string ((generator/generate "build/acceptance/ir/cli-start.json"
                                                            (str/replace ir-json "one" "uno")
                                                            "acceptance/generated")
                                        metadata-path))
                    "implementation_hash")))))

(describe "Command"
  (it "rejects wrong usage with exit code 2"
    (should= 2 (generator/run ["only-one"])))

  (it "reports unreadable input with exit code 1"
    (should= 1 (generator/run ["tmp/does-not-exist.json" "tmp/generated"]))))

# mutation-stamp: sha256=447a1e4b0d1fa0eacd7cdd884d13a920b671fe5bf61542f228a5ca8743ad5b92
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-09-24T06:31:07.406571Z","feature_name":"CLI invalid question","feature_path":"features/cli-invalid-question.feature","background_hash":"74234e98afe7498fb5daf1f36ac2d78acc339464f950703b8c019892f982b90b","implementation_hash":"sha256:3c5cdc8007f0032964d31c6980c71c7b03314156b121a1a803c23b99ab2cb118","scenarios":[]}
# acceptance-mutation-manifest-end

# Scenario: CLI invalid question 01 - unparseable free text costs no question
Feature: CLI invalid question
  Scenario: CLI invalid question 01 - unparseable free text costs no question
    Given I start "last-train --seed reference --voices=template"
    When I type "what is love?"
    Then I see "Signal's noisy. Rephrase."
    And the question count remains 3
    And I remain in Station 1

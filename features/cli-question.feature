# mutation-stamp: sha256=aaf903c156f3038ec3d55d838ee97ba488f73954c9e32e489344fd4d351f50e3
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-09-24T06:31:07.631693Z","feature_name":"CLI question","feature_path":"features/cli-question.feature","background_hash":"74234e98afe7498fb5daf1f36ac2d78acc339464f950703b8c019892f982b90b","implementation_hash":"sha256:f0ec9b8275620226426cd6c7a294603fdd45b7178a60bca0110317acb099f176","scenarios":[]}
# acceptance-mutation-manifest-end

# Scenario: CLI question 01 - a valid question advances the game and narrows the puzzle
Feature: CLI question
  Scenario: CLI question 01 - a valid question advances the game and narrows the puzzle
    Given I start "last-train --seed reference --voices=template"
    When I ask passenger B whether D is an Agent
    Then passenger B's template answer is no
    And the question count decreases to 2
    And the possible Agent seats are A and C
    And the game advances to the déjà vu round

# mutation-stamp: sha256=7db1869c6ae698cfcce0b298fc2e27a2e5e66fb79e14e7e713f8fb56d1fcdfc6
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-09-24T06:36:24.232296Z","feature_name":"Reference puzzle answers","feature_path":"features/reference-puzzle-answers.feature","background_hash":"74234e98afe7498fb5daf1f36ac2d78acc339464f950703b8c019892f982b90b","implementation_hash":"sha256:42e48662a058c01487c0ab97de5c43cc5065d9a1526abb3fcdfe69d7a42f85e6","scenarios":[]}
# acceptance-mutation-manifest-end

# Scenario: Reference puzzle answers 01 - two answers from B identify the Agent
Feature: Reference puzzle answers
  Scenario: Reference puzzle answers 01 - two answers from B identify the Agent
    Given the opening statements are the reference statements
    And the true passenger world is A=AGENT, B=SLEEPER, C=SLEEPER, D=AWAKE
    When I ask passenger B whether D is an Agent
    Then B answers no about D
    And the possible Agent seats are A and C
    When I ask passenger B whether C is an Agent
    Then B answers no about C
    And the only possible Agent seat is A

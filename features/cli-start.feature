# mutation-stamp: sha256=277e89ac90333eaea5cf3df11bdcd5786eaf97c5034e3c524677645c49bd77d9
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-09-24T06:31:07.743742Z","feature_name":"CLI start","feature_path":"features/cli-start.feature","background_hash":"74234e98afe7498fb5daf1f36ac2d78acc339464f950703b8c019892f982b90b","implementation_hash":"sha256:97f791c07c34ea87a69ad9f5e55e2ad6aedafffacfca91277173ee2a70dc2cfd","scenarios":[]}
# acceptance-mutation-manifest-end

# Scenario: CLI start 01 - reference seed starts with template boarding statements
Feature: CLI start
  Scenario: CLI start 01 - reference seed starts with template boarding statements
    When I start "last-train --seed reference --voices=template"
    Then the game displays 4 passenger statements
    And the statements convey the reference opening statements
    And the game has 3 questions available
    And the game does not display the true passenger roles

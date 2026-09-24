# mutation-stamp: sha256=7403ebcad68c5787785d1e6c7408dd8769bb907ec21cdfc6328c14eb16c672b1
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-09-24T22:53:00.121914Z","feature_name":"Web game start","feature_path":"features/web-game-start.feature","background_hash":"74234e98afe7498fb5daf1f36ac2d78acc339464f950703b8c019892f982b90b","implementation_hash":"sha256:a72d5d1ed890c5bde42f8e0f076d4601517d646b6a9c9a01b34ef7a4f2044313","scenarios":[]}
# acceptance-mutation-manifest-end

# Scenario: Web game start 01 - the browser page starts a playable reference game
Feature: Web game start
  Scenario: Web game start 01 - the browser page starts a playable reference game
    Given I open a new reference game in the web page
    Then I see the four passenger opening statements
    And I see that 3 questions remain
    And I do not see the passengers' true roles
    And I can type a question for a chosen passenger at the prompt
    And I can type an accusation with an optional ally at the prompt

# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-09-24T22:53:03.551469Z","feature_name":"Web game accusation","feature_path":"features/web-game-accusation.feature","background_hash":"74234e98afe7498fb5daf1f36ac2d78acc339464f950703b8c019892f982b90b","implementation_hash":"sha256:1a90a3bc1cb11f408aec82c90d32aaa06dbf6f93990bcd34276f2a1bd14d0d6b","scenarios":[]}
# acceptance-mutation-manifest-end

# Scenario: Web game accusation 01 - a correct Agent and ally produce a perfect run
Feature: Web game accusation
  Scenario Outline: Web game accusation 01 - a correct Agent and ally produce a perfect run
    Given I have started a reference game in the web page
    When I ask passenger B whether <first> and then <second> are Agents
    And I accuse passenger A and name passenger D as the Awake ally
    Then I see "PERFECT RUN"
    And the score is 175
    And the game is over

    Examples:
      | first | second |
      | D     | C      |

# mutation-stamp: sha256=dd9f7ace62edc2f61211718dcb763d2a0a95320710bf921962ee96acafdd565e
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-09-24T06:36:23.266784Z","feature_name":"CLI perfect run","feature_path":"features/cli-perfect-run.feature","background_hash":"74234e98afe7498fb5daf1f36ac2d78acc339464f950703b8c019892f982b90b","implementation_hash":"sha256:3be8202b603f90c10dab32077fade9f2551d262b0e2c02d864ab4021fd981977","scenarios":[]}
# acceptance-mutation-manifest-end

# Scenario: CLI perfect run 01 - correct Agent and Awake ally produce a perfect run
Feature: CLI perfect run
  Scenario: CLI perfect run 01 - correct Agent and Awake ally produce a perfect run
    Given I start "last-train --seed reference --voices=template"
    When I ask passenger B about D and then C as Agents
    Then passenger B answers no to both questions
    And I have 1 unused question
    When I accuse passenger A and name passenger D as the Awake ally
    Then I see "PERFECT RUN"
    And the score is 175
    And the game is over

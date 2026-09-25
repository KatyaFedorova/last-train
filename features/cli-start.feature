# Scenario: CLI start 01 - reference seed starts with template boarding statements
Feature: CLI start
  Scenario: CLI start 01 - reference seed starts with template boarding statements
    When I start "last-train --seed reference --voices=template"
    Then the game displays 4 passenger statements
    And the statements convey the reference opening statements
    And the game has 3 questions available
    And the game does not display the true passenger roles

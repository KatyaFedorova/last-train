# Scenario: CLI start 01 - a seeded run starts with four lines and one possible Agent
Feature: CLI start
  Scenario: CLI start 01 - a seeded run starts with four lines and one possible Agent
    When I start "last-train --seed 42"
    Then I see "Train 1 of 10"
    And the game displays 4 passenger statements
    And the statements leave exactly one possible Agent
    And the game does not display the true passenger roles

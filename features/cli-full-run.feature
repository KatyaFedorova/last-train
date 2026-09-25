# Scenario: CLI full run 01 - ten right answers end the run with a perfect tally
Feature: CLI full run
  Scenario: CLI full run 01 - ten right answers end the run with a perfect tally
    Given I start "last-train --seed 42"
    When I name the Agent on every train
    Then I see "Run over: 10 of 10 right."
    And the score is 1000
    And the game is over

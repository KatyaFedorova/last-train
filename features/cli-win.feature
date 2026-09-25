# Scenario: CLI win 01 - accusing the Agent wins and scores unused questions
Feature: CLI win
  Scenario: CLI win 01 - accusing the Agent wins and scores unused questions
    Given I start "last-train --seed reference --voices=template"
    When I ask passenger C whether A is the Agent
    And I accuse passenger A
    Then I see "WIN"
    And the score is 150
    And the game is over

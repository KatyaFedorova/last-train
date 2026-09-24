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

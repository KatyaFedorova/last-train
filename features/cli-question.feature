# Scenario: CLI question 01 - a valid question spends a question and narrows the puzzle
Feature: CLI question
  Scenario: CLI question 01 - a valid question spends a question and narrows the puzzle
    Given I start "last-train --seed reference --voices=template"
    When I ask passenger C whether A is the Agent
    Then passenger C's template answer is yes
    And the question count decreases to 2
    And the only possible Agent seat is A

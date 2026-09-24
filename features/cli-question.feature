# Scenario: CLI question 01 - a valid question advances the game and narrows the puzzle
Feature: CLI question
  Scenario: CLI question 01 - a valid question advances the game and narrows the puzzle
    Given I start "last-train --seed reference --voices=template"
    When I ask passenger B whether D is an Agent
    Then passenger B's template answer is no
    And the question count decreases to 2
    And the possible Agent seats are A and C
    And the game advances to the déjà vu round

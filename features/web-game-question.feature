# Scenario: Web game question 01 - submitting a valid question updates the game
Feature: Web game question
  Scenario: Web game question 01 - submitting a valid question updates the game
    Given I have started a reference game in the web page
    When I ask passenger B "Is D an Agent?"
    Then passenger B answers no
    And I see that 2 questions remain
    And the possible Agent seats are A and C
    And the game advances to the déjà vu round

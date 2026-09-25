# Scenario: Web game question 01 - submitting a valid question updates the game
Feature: Web game question
  Scenario: Web game question 01 - submitting a valid question updates the game
    Given I have started a reference game in the web page
    When I ask passenger B "Is D the Agent?"
    Then passenger B answers no
    And I see that 2 questions remain
    And the only possible Agent seat is A

# Scenario: Web game wrong accusation 01 - accusing the wrong passenger ends in a loss
Feature: Web game wrong accusation
  Scenario: Web game wrong accusation 01 - accusing the wrong passenger ends in a loss
    Given I have started a reference game in the web page
    When I accuse passenger D
    Then I see "LOSE"
    And the game is over

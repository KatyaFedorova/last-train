# Scenario: Web game win 01 - accusing the Agent after one question wins
Feature: Web game win
  Scenario: Web game win 01 - accusing the Agent after one question wins
    Given I have started a reference game in the web page
    When I ask passenger C "Is A the Agent?"
    And I accuse passenger A
    Then I see "WIN"
    And the score is 150
    And the game is over

# Scenario: Web game win 01 - a fast right answer earns a time bonus
Feature: Web game win
  Scenario: Web game win 01 - a fast right answer earns a time bonus
    Given I open the web page for seed 42
    When I name the Agent with 12 seconds left
    Then I see "Right!"
    And I see "+160"

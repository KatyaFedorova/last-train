# Scenario: Web game wrong accusation 01 - a wrong answer shows the Agent's lie
Feature: Web game wrong accusation
  Scenario: Web game wrong accusation 01 - a wrong answer shows the Agent's lie
    Given I open the web page for seed 42
    When I name a passenger who is not the Agent
    Then I see "Wrong."
    And I see the Agent's lie

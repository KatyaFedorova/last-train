# Scenario: Web game timeout 01 - running out of time shows the Agent's lie
Feature: Web game timeout
  Scenario: Web game timeout 01 - running out of time shows the Agent's lie
    Given I open the web page for seed 42
    When the clock runs out
    Then I see "Time's up!"
    And I see the Agent's lie

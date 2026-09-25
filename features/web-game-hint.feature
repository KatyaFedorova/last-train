# Scenario: Web game hint 01 - one tip per run names an honest passenger
Feature: Web game hint
  Scenario: Web game hint 01 - one tip per run names an honest passenger
    Given I open the web page for seed 42
    When I type "hint" at the prompt
    Then the tip names a passenger who is not the Agent
    When I type "hint" at the prompt
    Then I see "No hints left on this run."

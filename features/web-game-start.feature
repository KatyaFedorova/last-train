# Scenario: Web game start 01 - a seeded link starts a playable run
Feature: Web game start
  Scenario: Web game start 01 - a seeded link starts a playable run
    Given I open the web page for seed 42
    Then I see the four passenger lines
    And I see "Train 1 of 10"
    And I do not see the passengers' true roles
    And I can tap each passenger or press their letter

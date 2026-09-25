# Scenario: CLI wrong accusation 01 - a wrong answer explains itself and shows the Agent's lie
Feature: CLI wrong accusation
  Scenario: CLI wrong accusation 01 - a wrong answer explains itself and shows the Agent's lie
    Given I start "last-train --seed 42"
    When I name a passenger who is not the Agent
    Then I see "Wrong."
    And I see the Agent's lie
    And I see "Train 2 of 3"

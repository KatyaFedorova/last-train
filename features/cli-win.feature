# Scenario: CLI win 01 - naming the Agent scores and deals the next train
Feature: CLI win
  Scenario: CLI win 01 - naming the Agent scores and deals the next train
    Given I start "last-train --seed 42"
    When I name the Agent
    Then I see "Right!"
    And I see "Train 2 of 3"

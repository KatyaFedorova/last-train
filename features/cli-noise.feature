# Scenario: CLI noise 01 - text that names nobody changes nothing
Feature: CLI noise
  Scenario: CLI noise 01 - text that names nobody changes nothing
    Given I start "last-train --seed 42"
    When I type "what is love?"
    Then I see "Signal's noisy. Press A, B, C or D, or type hint."
    And I am still on train 1

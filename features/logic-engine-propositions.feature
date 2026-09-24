# Scenario: Logic engine propositions 01 - supported propositions use ordinary boolean evaluation
Feature: Logic engine propositions
  Scenario: Logic engine propositions 01 - supported propositions use ordinary boolean evaluation
    Given the 12 possible passenger worlds
    When I evaluate Is, Same, CountEq, Not, And, and Or propositions in each world
    Then each proposition has its ordinary boolean value in that world

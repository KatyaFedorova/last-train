# Scenario: Evaluate supported propositions across every passenger world
Feature: Logic engine propositions
  Scenario: Evaluate supported propositions across every passenger world
    Given the 12 possible passenger worlds
    When I evaluate Is, Same, CountEq, Not, And, and Or propositions in each world
    Then each proposition has its ordinary boolean value in that world

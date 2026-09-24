# Scenario: Passenger answer invariants 01 - Agents invert Awake answers in every world
Feature: Passenger answer invariants
  Scenario: Passenger answer invariants 01 - Agents invert Awake answers in every world
    Given the 12 possible passenger worlds
    When the Agent and Awake passenger answer the same supported proposition in each world
    Then their answers are opposite in every world

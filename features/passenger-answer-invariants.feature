# Scenario: Passenger answer invariants 01 - the Agent inverts human answers in every world
Feature: Passenger answer invariants
  Scenario: Passenger answer invariants 01 - the Agent inverts human answers in every world
    Given the 4 possible passenger worlds
    When the Agent and a human passenger answer the same supported proposition in each world
    Then their answers are opposite in every world

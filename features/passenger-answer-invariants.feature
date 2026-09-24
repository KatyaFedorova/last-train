# Scenario: Agent answers are the opposite of Awake answers
Feature: Passenger answer invariants
  Scenario: Agent answers are the opposite of Awake answers
    Given the 12 possible passenger worlds
    When the Agent and Awake passenger answer the same supported proposition in each world
    Then their answers are opposite in every world

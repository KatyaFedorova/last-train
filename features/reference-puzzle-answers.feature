# Scenario: Reference puzzle answers 01 - two answers from B identify the Agent
Feature: Reference puzzle answers
  Scenario: Reference puzzle answers 01 - two answers from B identify the Agent
    Given the opening statements are the reference statements
    And the true passenger world is A=AGENT, B=SLEEPER, C=SLEEPER, D=AWAKE
    When I ask passenger B whether D is an Agent
    Then B answers no about D
    And the possible Agent seats are A and C
    When I ask passenger B whether C is an Agent
    Then B answers no about C
    And the only possible Agent seat is A

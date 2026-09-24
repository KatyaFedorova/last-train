# Scenario: Passenger answer rules 01 - each kind answers supported propositions by its rule
Feature: Passenger answer rules
  Background:
    Given the passenger world is A=AGENT, B=SLEEPER, C=SLEEPER, D=AWAKE

  Scenario Outline: Passenger answer rules 01 - each kind answers supported propositions by its rule
    When I ask passenger <speaker> whether <proposition>
    Then the answer is <answer>

    Examples:
      | speaker | proposition                         | answer |
      | D       | A is an Agent                       | yes    |
      | D       | B is an Agent                       | no     |
      | A       | A is an Agent                       | no     |
      | A       | B is an Agent                       | yes    |
      | B       | A is an Agent                       | no     |
      | B       | A is Awake                          | no     |
      | B       | A is a Sleeper                      | yes    |
      | B       | C and D are the same kind           | yes    |
      | B       | exactly 0 passengers are Agents     | yes    |
      | B       | it is not true that A is an Agent   | yes    |
      | B       | A is an Agent and D is Awake        | no     |
      | B       | A is a Sleeper and D is a Sleeper   | yes    |
      | A       | A is an Agent and D is Awake        | no     |
      | A       | A is an Agent or B is an Agent      | no     |
      | D       | A is an Agent or B is an Agent      | yes    |

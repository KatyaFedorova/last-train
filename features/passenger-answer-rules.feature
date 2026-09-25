# Scenario: Passenger answer rules 01 - humans tell the truth and the Agent lies
Feature: Passenger answer rules
  Background:
    Given the passenger world is A=AGENT, B=HUMAN, C=HUMAN, D=HUMAN

  Scenario Outline: Passenger answer rules 01 - humans tell the truth and the Agent lies
    When I ask passenger <speaker> whether <proposition>
    Then the answer is <answer>

    Examples:
      | speaker | proposition                   | answer |
      | D       | A is the Agent                | yes    |
      | D       | B is the Agent                | no     |
      | A       | A is the Agent                | no     |
      | A       | B is the Agent                | yes    |
      | B       | A is not the Agent            | no     |
      | A       | A is not the Agent            | yes    |
      | C       | D is human                    | yes    |
      | A       | D is human                    | no     |
      | C       | A is the Agent and D is human | yes    |
      | A       | A is the Agent and D is human | no     |
      | D       | B or C is the Agent           | no     |
      | A       | B or C is the Agent           | yes    |

# Scenario: Sleeper answer invariants 01 - answer from the all-Sleeper view
Feature: Sleeper answer invariants
  Scenario Outline: Sleeper answer invariants 01 - answer from the all-Sleeper view
    Given the 12 possible passenger worlds
    When each Sleeper is asked whether <proposition>
    Then every answer is <answer>

    Examples:
      | proposition                         | answer |
      | A is an Agent                        | no     |
      | B is an Agent                        | no     |
      | C is an Agent                        | no     |
      | D is an Agent                        | no     |
      | A and B are the same kind            | yes    |
      | A and C are the same kind            | yes    |
      | A and D are the same kind            | yes    |
      | B and C are the same kind            | yes    |
      | B and D are the same kind            | yes    |
      | C and D are the same kind            | yes    |

# Scenario: Reference puzzle solvability 01 - one question to a trusted passenger suffices
Feature: Reference puzzle solvability
  Scenario Outline: Reference puzzle solvability 01 - one question to a trusted passenger suffices
    Given the possible worlds are the two reference worlds
    Then the reference puzzle solvability at depth <depth> is <solvable>

    Examples:
      | depth | solvable |
      | 0     | no       |
      | 1     | yes      |

# Scenario: Reference puzzle solvability 01 - check whether one or two adaptive questions suffice
Feature: Reference puzzle solvability
  Scenario Outline: Reference puzzle solvability 01 - check whether one or two adaptive questions suffice
    Given the possible worlds are the three reference worlds
    Then the reference puzzle solvability at depth <depth> is <solvable>

    Examples:
      | depth | solvable |
      | 1     | no       |
      | 2     | yes      |

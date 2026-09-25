# Scenario: Puzzle generator 01 - every train of a run leaves exactly one possible Agent
# Scenario: Puzzle generator 02 - the same seed replays the same run
Feature: Puzzle generator
  Scenario Outline: Puzzle generator 01 - every train of a run leaves exactly one possible Agent
    Given the run seeded <seed>
    Then every train of the run leaves exactly one possible Agent

    Examples:
      | seed      |
      | 1         |
      | 42        |
      | 999999999 |

  Scenario: Puzzle generator 02 - the same seed replays the same run
    Given the run seeded 42
    Then a second run seeded 42 deals the same trains

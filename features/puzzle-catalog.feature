# Scenario: Puzzle catalog 01 - every named puzzle follows the puzzle rules
# Scenario: Puzzle catalog 02 - a new game never repeats the puzzle just played
Feature: Puzzle catalog
  Scenario Outline: Puzzle catalog 01 - every named puzzle follows the puzzle rules
    Given the puzzle named "<name>"
    Then the puzzle follows the puzzle rules

    Examples:
      | name        |
      | reference   |
      | commuters   |
      | night-shift |
      | terminus    |
      | red-eye     |

  Scenario: Puzzle catalog 02 - a new game never repeats the puzzle just played
    Given I just played the puzzle named "reference"
    When a new game picks a puzzle
    Then it is not "reference"

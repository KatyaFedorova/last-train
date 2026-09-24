# Scenario: Reference puzzle worlds 01 - opening statements leave three Agent candidates
Feature: Reference puzzle worlds
  Scenario: Reference puzzle worlds 01 - opening statements leave three Agent candidates
    Given the opening statements are
      | speaker | proposition                     | polarity |
      | A       | exactly 0 passengers are Agents | true     |
      | B       | A is not an Agent               | true     |
      | C       | B and C are the same kind       | true     |
      | D       | B and C are the same kind       | true     |
    When I inspect the possible worlds
    Then the possible Agent seats are A, C, and D
    And there are exactly 3 possible worlds

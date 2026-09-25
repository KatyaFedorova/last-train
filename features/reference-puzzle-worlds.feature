# Scenario: Reference puzzle worlds 01 - opening statements leave two Agent suspects
Feature: Reference puzzle worlds
  Scenario: Reference puzzle worlds 01 - opening statements leave two Agent suspects
    Given the opening statements are
      | speaker | proposition    | polarity |
      | A       | B is the Agent | true     |
      | B       | A is the Agent | true     |
      | C       | C is the Agent | false    |
      | D       | C is the Agent | false    |
    When I inspect the possible worlds
    Then the possible Agent seats are A and B
    And there are exactly 2 possible worlds

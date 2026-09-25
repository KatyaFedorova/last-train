# Scenario: Reference puzzle worlds 01 - opening lines leave exactly one Agent
Feature: Reference puzzle worlds
  Scenario: Reference puzzle worlds 01 - opening lines leave exactly one Agent
    Given the opening statements are
      | speaker | proposition    | polarity |
      | A       | B is the Agent | true     |
      | B       | A is the Agent | true     |
      | C       | A is the Agent | true     |
      | D       | D is the Agent | false    |
    When I inspect the possible worlds
    Then the only possible Agent seat is A
    And there are exactly 1 possible worlds

# Scenario: Web game accusation 01 - a correct Agent and ally produce a perfect run
Feature: Web game accusation
  Scenario Outline: Web game accusation 01 - a correct Agent and ally produce a perfect run
    Given I have started a reference game in the web page
    When I ask passenger B whether <first> and then <second> are Agents
    And I accuse passenger A and name passenger D as the Awake ally
    Then I see "PERFECT RUN"
    And the score is 175
    And the game is over

    Examples:
      | first | second |
      | D     | C      |

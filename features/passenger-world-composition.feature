# Scenario: Passenger world composition 01 - one Agent and three humans
Feature: Passenger world composition
  Scenario Outline: Passenger world composition 01 - one Agent and three humans
    Given the passenger world is A=<a>, B=<b>, C=<c>, D=<d>
    Then the world has exactly 1 Agent and 3 humans

    Examples:
      | a     | b     | c     | d     |
      | AGENT | HUMAN | HUMAN | HUMAN |
      | HUMAN | AGENT | HUMAN | HUMAN |
      | HUMAN | HUMAN | AGENT | HUMAN |
      | HUMAN | HUMAN | HUMAN | AGENT |

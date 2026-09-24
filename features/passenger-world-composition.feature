# Scenario: Passenger world composition 01 - one Agent, one Awake passenger, and two Sleepers
Feature: Passenger world composition
  Scenario Outline: Passenger world composition 01 - one Agent, one Awake passenger, and two Sleepers
    Given the passenger world is A=<a>, B=<b>, C=<c>, D=<d>
    Then the world has exactly 1 Agent, 1 Awake passenger, and 2 Sleepers

    Examples:
      | a       | b       | c       | d       |
      | AGENT   | AWAKE   | SLEEPER | SLEEPER |
      | AGENT   | SLEEPER | AWAKE   | SLEEPER |
      | AGENT   | SLEEPER | SLEEPER | AWAKE   |
      | AWAKE   | AGENT   | SLEEPER | SLEEPER |
      | SLEEPER | AGENT   | AWAKE   | SLEEPER |
      | SLEEPER | AGENT   | SLEEPER | AWAKE   |
      | AWAKE   | SLEEPER | AGENT   | SLEEPER |
      | SLEEPER | AWAKE   | AGENT   | SLEEPER |
      | SLEEPER | SLEEPER | AGENT   | AWAKE   |
      | AWAKE   | SLEEPER | SLEEPER | AGENT   |
      | SLEEPER | AWAKE   | SLEEPER | AGENT   |
      | SLEEPER | SLEEPER | AWAKE   | AGENT   |

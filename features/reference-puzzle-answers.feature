# Scenario: Reference puzzle answers 01 - asking a trusted passenger identifies the Agent
Feature: Reference puzzle answers
  Scenario: Reference puzzle answers 01 - asking a trusted passenger identifies the Agent
    Given the opening statements are the reference statements
    And the true passenger world is A=AGENT, B=HUMAN, C=HUMAN, D=HUMAN
    When I ask passenger A whether B is the Agent
    Then A answers yes about B
    And the possible Agent seats are A and B
    When I ask passenger C whether A is the Agent
    Then C answers yes about A
    And the only possible Agent seat is A

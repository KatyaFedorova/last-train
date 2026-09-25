# Scenario: CLI wrong accusation 01 - accusing the wrong passenger loses and reveals the Agent
Feature: CLI wrong accusation
  Scenario: CLI wrong accusation 01 - accusing the wrong passenger loses and reveals the Agent
    Given I start "last-train --seed reference --voices=template"
    When I accuse passenger D
    Then I see "LOSE"
    And I see "Vera stands up"
    And the game is over

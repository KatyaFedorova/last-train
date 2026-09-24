# Scenario: CLI wrong accusation 01 - accusing the wrong passenger loses
Feature: CLI wrong accusation
  Scenario: CLI wrong accusation 01 - accusing the wrong passenger loses
    Given I start "last-train --seed reference --voices=template"
    When I accuse passenger D
    Then I see "LOSE"
    And the game is over

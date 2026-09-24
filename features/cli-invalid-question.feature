# Scenario: CLI invalid question 01 - unparseable free text costs no question
Feature: CLI invalid question
  Scenario: CLI invalid question 01 - unparseable free text costs no question
    Given I start "last-train --seed reference --voices=template"
    When I type "what is love?"
    Then I see "Signal's noisy. Rephrase."
    And the question count remains 3

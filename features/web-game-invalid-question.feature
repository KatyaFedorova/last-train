# Scenario: Web game invalid question 01 - an unsupported question is rejected without cost
Feature: Web game invalid question
  Scenario: Web game invalid question 01 - an unsupported question is rejected without cost
    Given I have started a reference game in the web page
    When I enter "what is love?" as a question for passenger B
    Then I see "Signal's noisy. Rephrase."
    And I see that 3 questions remain

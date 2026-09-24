# Scenario: Web game start 01 - the browser page starts a playable reference game
Feature: Web game start
  Scenario: Web game start 01 - the browser page starts a playable reference game
    Given I open a new reference game in the web page
    Then I see the four passenger opening statements
    And I see that 3 questions remain
    And I do not see the passengers' true roles
    And I can choose a passenger and enter a yes-or-no question
    And I can accuse a passenger and optionally name an ally

# Scenario: Web game terminal 01 - an unrecognised command shows the command syntax
# Scenario: Web game terminal 02 - new starts a different puzzle
# Scenario: Web game terminal 03 - an unknown puzzle link boards a random train
Feature: Web game terminal
  Scenario: Web game terminal 01 - an unrecognised command shows the command syntax
    Given I have started a reference game in the web page
    When I type "hello" at the prompt
    Then I see "Signal's noisy. Rephrase."
    And I see "Accuse: accuse <passenger>"
    And I see that 3 questions remain

  Scenario: Web game terminal 02 - new starts a different puzzle
    Given I have started a reference game in the web page
    When I accuse passenger D
    And I type "new" at the prompt
    Then a different puzzle starts
    And I see that 3 questions remain

  Scenario: Web game terminal 03 - an unknown puzzle link boards a random train
    Given I open the web page for the puzzle named "nope"
    Then I see that 3 questions remain
    And I see "Unknown puzzle "nope". Boarding a random train."

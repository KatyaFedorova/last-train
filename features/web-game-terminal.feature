# Scenario: Web game terminal 01 - unrecognised input is noise
# Scenario: Web game terminal 02 - new starts a fresh run
# Scenario: Web game terminal 03 - a link without a usable seed boards a random run
Feature: Web game terminal
  Scenario: Web game terminal 01 - unrecognised input is noise
    Given I open the web page for seed 42
    When I type "hello" at the prompt
    Then I see "Signal's noisy."
    And I am still on train 1

  Scenario: Web game terminal 02 - new starts a fresh run
    Given I open the web page for seed 42
    When I name the Agent
    And I type "new" at the prompt
    Then I see "Train 1 of 10"
    And I am still on train 1

  Scenario: Web game terminal 03 - a link without a usable seed boards a random run
    Given I open the web page for seed "nope"
    Then I see "Train 1 of 10"

# Scenario: Web game README 01 - README explains how to run and test browser play
Feature: Web game README
  Scenario: Web game README 01 - README explains how to run and test browser play
    Given I read the project README
    Then it explains how to start the web game locally
    And it gives the local address to open in a browser
    And it names the web acceptance test command

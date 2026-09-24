# mutation-stamp: sha256=8c5b6a9d06effd3d6fd924593fe691d2ace225566254c75a9a708adef769c070
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-09-24T22:53:21.175926Z","feature_name":"Passenger answer rules","feature_path":"features/passenger-answer-rules.feature","background_hash":"7b62a40cd5758167d3424c162e5bff2b8b32e2abfb66a83ab8648868d508380f","implementation_hash":"sha256:db2d9f18a9bb17dcc745c30e43cb389ecac652821f0c07355b5ecac3b9974e3a","scenarios":[{"index":0,"name":"Passenger answer rules 01 - each kind answers supported propositions by its rule","scenario_hash":"315fa0972cf794bd6cfb85fc6d348b9d27fbf410d76b45554f242dd9ac6dac26","mutation_count":54,"result":{"Total":54,"Killed":54,"Survived":0,"Errors":0},"tested_at":"2026-09-24T22:52:06.259735Z"}]}
# acceptance-mutation-manifest-end

# Scenario: Passenger answer rules 01 - each kind answers supported propositions by its rule
Feature: Passenger answer rules
  Background:
    Given the passenger world is A=AGENT, B=SLEEPER, C=SLEEPER, D=AWAKE

  Scenario Outline: Passenger answer rules 01 - each kind answers supported propositions by its rule
    When I ask passenger <speaker> whether <proposition>
    Then the answer is <answer>

    Examples:
      | speaker | proposition                         | answer |
      | D       | A is an Agent                       | yes    |
      | D       | B is an Agent                       | no     |
      | A       | A is an Agent                       | no     |
      | A       | B is an Agent                       | yes    |
      | B       | A is an Agent                       | no     |
      | B       | A is Awake                          | no     |
      | B       | A is a Sleeper                      | yes    |
      | B       | C and D are the same kind           | yes    |
      | B       | There are no Agents on this train   | yes    |
      | B       | A is not an Agent                   | yes    |
      | B       | B is an Agent                       | no     |
      | B       | B is Awake                          | no     |
      | B       | B is a Sleeper                      | yes    |
      | B       | A is an Agent and D is Awake        | no     |
      | B       | A is a Sleeper and D is a Sleeper   | yes    |
      | A       | A is an Agent and D is Awake        | no     |
      | A       | A is an Agent or B is an Agent      | no     |
      | D       | A is an Agent or B is an Agent      | yes    |

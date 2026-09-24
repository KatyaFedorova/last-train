# mutation-stamp: sha256=50c5ae608181044b6a20c96217e74effbd517f5b57772dd0fb5484d7e78174fb
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-09-24T06:36:23.973419Z","feature_name":"Passenger answer rules","feature_path":"features/passenger-answer-rules.feature","background_hash":"7b62a40cd5758167d3424c162e5bff2b8b32e2abfb66a83ab8648868d508380f","implementation_hash":"sha256:aeb8a53f6e141a5387cf75dc04135e64823e9224b46c97ce8f867773c28308cd","scenarios":[{"index":0,"name":"Passenger answer rules 01 - each kind answers supported propositions by its rule","scenario_hash":"77b379c1f90d52a2e07b2b9fef7f65e28d9f6b37f465282bbce380c5c67c29f8","mutation_count":45,"result":{"Total":45,"Killed":45,"Survived":0,"Errors":0},"tested_at":"2026-09-24T06:24:53.066652Z"}]}
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

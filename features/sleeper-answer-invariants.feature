# mutation-stamp: sha256=8fae7d53bc4352b4de56c34911ad4f163165ee7634086456fd4e75244aa50e7b
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-09-24T06:36:24.579621Z","feature_name":"Sleeper answer invariants","feature_path":"features/sleeper-answer-invariants.feature","background_hash":"74234e98afe7498fb5daf1f36ac2d78acc339464f950703b8c019892f982b90b","implementation_hash":"sha256:527050ed2686a114fcbe55701d1399202f25f8f82c5840ff3a722fe6823f3143","scenarios":[{"index":0,"name":"Sleeper answer invariants 01 - answer from the all-Sleeper view","scenario_hash":"056819f4d464633d45401fdcf33ceee8db0021ab963bf6282e15707a6cbc0bb4","mutation_count":20,"result":{"Total":20,"Killed":20,"Survived":0,"Errors":0},"tested_at":"2026-09-24T06:27:12.674677Z"}]}
# acceptance-mutation-manifest-end

# Scenario: Sleeper answer invariants 01 - answer from the all-Sleeper view
Feature: Sleeper answer invariants
  Scenario Outline: Sleeper answer invariants 01 - answer from the all-Sleeper view
    Given the 12 possible passenger worlds
    When each Sleeper is asked whether <proposition>
    Then every answer is <answer>

    Examples:
      | proposition                         | answer |
      | A is an Agent                        | no     |
      | B is an Agent                        | no     |
      | C is an Agent                        | no     |
      | D is an Agent                        | no     |
      | A and B are the same kind            | yes    |
      | A and C are the same kind            | yes    |
      | A and D are the same kind            | yes    |
      | B and C are the same kind            | yes    |
      | B and D are the same kind            | yes    |
      | C and D are the same kind            | yes    |

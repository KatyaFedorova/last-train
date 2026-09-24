# mutation-stamp: sha256=1754953cf640cb698149844dbba9a7c2c2feea15ee91520ac04e326b92d24a88
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-09-24T06:36:24.347767Z","feature_name":"Reference puzzle solvability","feature_path":"features/reference-puzzle-solvability.feature","background_hash":"74234e98afe7498fb5daf1f36ac2d78acc339464f950703b8c019892f982b90b","implementation_hash":"sha256:ff8c8300a9bbce8971fae8b41e3181e29d0c1a56f70c56111b82bb4dd638baf3","scenarios":[{"index":0,"name":"Reference puzzle solvability 01 - check whether one or two adaptive questions suffice","scenario_hash":"9ea9ec78044cf87acb0a873f3f445b71ed392512992139c003ec2cf06db107cf","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-09-24T06:28:33.624263Z"}]}
# acceptance-mutation-manifest-end

# Scenario: Reference puzzle solvability 01 - check whether one or two adaptive questions suffice
Feature: Reference puzzle solvability
  Scenario Outline: Reference puzzle solvability 01 - check whether one or two adaptive questions suffice
    Given the possible worlds are the three reference worlds
    Then the reference puzzle solvability at depth <depth> is <solvable>

    Examples:
      | depth | solvable |
      | 1     | no       |
      | 2     | yes      |

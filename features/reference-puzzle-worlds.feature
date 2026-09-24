# mutation-stamp: sha256=0d09e689bf8805bbf8cb48dbb756b58379031671370e54cda865c7d51a44fe8e
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-09-24T06:31:08.686511Z","feature_name":"Reference puzzle worlds","feature_path":"features/reference-puzzle-worlds.feature","background_hash":"74234e98afe7498fb5daf1f36ac2d78acc339464f950703b8c019892f982b90b","implementation_hash":"sha256:b753acc1d738cb0c1fbc0b7fb6198f4d0bc90d359171ed1b2caeb460c891b095","scenarios":[]}
# acceptance-mutation-manifest-end

# Scenario: Reference puzzle worlds 01 - opening statements leave three Agent candidates
Feature: Reference puzzle worlds
  Scenario: Reference puzzle worlds 01 - opening statements leave three Agent candidates
    Given the opening statements are
      | speaker | proposition                     | polarity |
      | A       | exactly 0 passengers are Agents | true     |
      | B       | A is not an Agent               | true     |
      | C       | B and C are the same kind       | true     |
      | D       | B and C are the same kind       | true     |
    When I inspect the possible worlds
    Then the possible Agent seats are A, C, and D
    And there are exactly 3 possible worlds

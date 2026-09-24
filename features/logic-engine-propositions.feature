# mutation-stamp: sha256=3a25600d56d1a4f1299e5182d4b69c8f069175cd8a17e39c4ac74c3e0005486e
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-09-24T06:36:23.730084Z","feature_name":"Logic engine propositions","feature_path":"features/logic-engine-propositions.feature","background_hash":"74234e98afe7498fb5daf1f36ac2d78acc339464f950703b8c019892f982b90b","implementation_hash":"sha256:ed3799efeae9c8c58e34499accd9aa63db72fee7cdf77281e384fa6164b79705","scenarios":[]}
# acceptance-mutation-manifest-end

# Scenario: Logic engine propositions 01 - supported propositions use ordinary boolean evaluation
Feature: Logic engine propositions
  Scenario: Logic engine propositions 01 - supported propositions use ordinary boolean evaluation
    Given the 12 possible passenger worlds
    When I evaluate Is, Same, CountEq, Not, And, and Or propositions in each world
    Then each proposition has its ordinary boolean value in that world

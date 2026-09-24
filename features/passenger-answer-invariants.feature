# mutation-stamp: sha256=acf87a6dce5f1db34a391c9d54c44910efe6fd9ae671623f5c72650a5f867c3f
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-09-24T06:31:08.079878Z","feature_name":"Passenger answer invariants","feature_path":"features/passenger-answer-invariants.feature","background_hash":"74234e98afe7498fb5daf1f36ac2d78acc339464f950703b8c019892f982b90b","implementation_hash":"sha256:c079be11ec7821162d86dec2be602ac9f60185319205204cf82435beed14cccb","scenarios":[]}
# acceptance-mutation-manifest-end

# Scenario: Passenger answer invariants 01 - Agents invert Awake answers in every world
Feature: Passenger answer invariants
  Scenario: Passenger answer invariants 01 - Agents invert Awake answers in every world
    Given the 12 possible passenger worlds
    When the Agent and Awake passenger answer the same supported proposition in each world
    Then their answers are opposite in every world

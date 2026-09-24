# mutation-stamp: sha256=bfbfe6ebbd3c0edd076aedd915d3cdf4e87bf2f06b0a5a43896a8e1c1a6f9e99
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-09-24T06:36:23.615154Z","feature_name":"CLI wrong accusation","feature_path":"features/cli-wrong-accusation.feature","background_hash":"74234e98afe7498fb5daf1f36ac2d78acc339464f950703b8c019892f982b90b","implementation_hash":"sha256:c20b9154e52eb0755ecd5c455155c35473bb1db4fc128bad9e5a67b96a4584af","scenarios":[]}
# acceptance-mutation-manifest-end

# Scenario: CLI wrong accusation 01 - accusing the wrong passenger loses
Feature: CLI wrong accusation
  Scenario: CLI wrong accusation 01 - accusing the wrong passenger loses
    Given I start "last-train --seed reference --voices=template"
    When I accuse passenger D
    Then I see "LOSE"
    And the game is over

# mutation-stamp: sha256=b39f6bbbc04ffef1610a49c23284c3a0e403b2bd3322b69d625d120596c83303
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-09-24T22:53:28.303274Z","feature_name":"Passenger world composition","feature_path":"features/passenger-world-composition.feature","background_hash":"74234e98afe7498fb5daf1f36ac2d78acc339464f950703b8c019892f982b90b","implementation_hash":"sha256:d1b2ee79ca6b44a8968ae56ecb7660a04328b5994de2d1b74c31d134938783fc","scenarios":[{"index":0,"name":"Passenger world composition 01 - one Agent, one Awake passenger, and two Sleepers","scenario_hash":"8b881bd0ca1783a5a6c7655c3c9c98dc5b89c4f9028d2eea792d6a6faad18e23","mutation_count":48,"result":{"Total":48,"Killed":48,"Survived":0,"Errors":0},"tested_at":"2026-09-24T06:27:12.194693Z"}]}
# acceptance-mutation-manifest-end

# Scenario: Passenger world composition 01 - one Agent, one Awake passenger, and two Sleepers
Feature: Passenger world composition
  Scenario Outline: Passenger world composition 01 - one Agent, one Awake passenger, and two Sleepers
    Given the passenger world is A=<a>, B=<b>, C=<c>, D=<d>
    Then the world has exactly 1 Agent, 1 Awake passenger, and 2 Sleepers

    Examples:
      | a       | b       | c       | d       |
      | AGENT   | AWAKE   | SLEEPER | SLEEPER |
      | AGENT   | SLEEPER | AWAKE   | SLEEPER |
      | AGENT   | SLEEPER | SLEEPER | AWAKE   |
      | AWAKE   | AGENT   | SLEEPER | SLEEPER |
      | SLEEPER | AGENT   | AWAKE   | SLEEPER |
      | SLEEPER | AGENT   | SLEEPER | AWAKE   |
      | AWAKE   | SLEEPER | AGENT   | SLEEPER |
      | SLEEPER | AWAKE   | AGENT   | SLEEPER |
      | SLEEPER | SLEEPER | AGENT   | AWAKE   |
      | AWAKE   | SLEEPER | SLEEPER | AGENT   |
      | SLEEPER | AWAKE   | SLEEPER | AGENT   |
      | SLEEPER | SLEEPER | AWAKE   | AGENT   |

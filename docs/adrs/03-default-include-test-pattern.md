# Title
Default include patterns for running tests. 

## Status
Accepted

## Context
By default, *Maven* runs only test classes suffixed with `Test`.
In the opposite, IDEs and *Gradle* runs all tests.

## Decision
Jeka will runs all the tests by default as it is consistent with IDE and not an isolated behavior 
in the java build tool landscape.

## Consequences
When running `jeka project: test`, all tests are executed unless explicitly specified.
# Title
Property Name for Skipping Tests

## Status
Accepted

## Context
A unique property is required for skipping tests in both projects and bases. This ensures the same property can be applied when running commands like `jeka base:pack` or `jeka project:pack`. 
It also simplifies invoking builds directly from OS scripts.

## Decision
The property will be named `jeka.test.skip`, aligning with the `$Maven$` equivalent `maven.skip.test`.

## Consequences
To build without running tests, use the following commands:
- `jeka project:pack -Djeka.test.skip`
- `jeka base:pack -Djeka.test.skip`
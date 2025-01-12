# Title
Run All Tests When the Include Pattern Is Empty

## Status
Accepted

## Context
Test selection allows filtering tests based on class names or tag presence.  
How should we interpret the behavior when no include pattern is specified?  
Should it mean "include all" or "exclude all"?

At first glance, "exclude all" might seem more logical.  
However, the JUnit platform considers an empty include pattern to mean "include all."

## Decision
We decided to follow the JUnit platform behavior, both for class names and tags.

## Consequences
Include all tests when the include pattern is empty.
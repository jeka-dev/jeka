# Title
Command line KBean notation

## Status
Accepted

## Context
We need to differentiate KBean, methods, and properties when parsing command lines. 

KBean names can be all lowercase, causing confusion with method names. 
Using `@KBeanName method1 method2 ...` might face conflict with [@file-arguments](https://picocli.info/#AtFiles) 
and require the *shift* key on some keyboards. 

However, a colon as `KBeanName: method1 method2 ...` doesn't need the `shift` key, despite being less noticeable.

## Decision
Jeka adopts the colon notation for KBean.

## Consequences

To mention default KBean, we'll simply use `:` as in `jeka project:pack : e2e`

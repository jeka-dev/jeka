# Migration Guide

This page helps you migrate from JeKa 0.10 to JeKa 0.11.

## Command Line and Wrapper

The command line has been unified: `jekaw` has been replaced by `jeka`.

The wrapper is now entirely included in the shell scripts.

The `jeka.version` property is now located in the `jeka.properties` file along with other properties.

## Project Structure

The nested `jeka` directory has been removed in favor of a flat structure.

Also, the entire wrapper is contained inside shell scripts, so the `jeka/wrapper` directory is no longer needed.

- `jeka/def` directory -> `jeka-src`
- `jeka/output` -> `jeka-output`
- `jeka/.work` -> `.jeka-work`
- `jeka/local.properties` -> `jeka.properties`
- `jekaw.bat` -> `jeka.ps1`
- `jekaw` -> `jeka`
- `jeka/project-dependencies.txt` -> `jeka.project.deps`
- `jeka/project-libs` directory -> `libs`


## Major API Changes

- KBean classes are now suffixed with `KBean` instead of `JkBean` (e.g., `ProjectKBean`).
- `JkBean.getBean()` -> `KBean.load()`

## Properties

- `jeka.classpath.inject` -> `jeka.inject.classpath`
- KBean field reference has changed from `myKBean#xxx` to `@myKBean.xxx`.

## Command Line

- KBean method invocation has changed from `myKBean#pack` to `myKBean: pack`.
  
  Example: `project#pack project#tests.skip=true`
  
  Is now: `project: pack tests.skip=true` 

- Use `jeka --help` as a starting point for command-line help.
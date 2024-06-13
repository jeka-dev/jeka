# Migration Guide

This page helps to migrate from JeKa 0.10 to Jeka 0.11

## Command line and wrapper

Command line has been unified : `jekaw` has been replaced by `jeka` only.

The thing is that the wrapper is now entirely included in the shell scripts.

The `jeka.version` is now located in *jeka.properties* file along other properties.

## Project Structure

*jeka* dir nested level has been suppressed in favor of a flat structure.

Also the entire wrapper is contained inside shell scripts so we don't need the *jeka/wrapper* dir anymore.

- *jeka/def* dir -> *jeka-src*
- *jeka/output* -> *jeka-output*
- *jeka/.work* -> *.jeka-work*
- *jeka/local.properties* -> *jeka.properties*
- *jekaw.bat* -> *jeka.ps1*
- *jekaw* -> *jeka*
- *jeka/project-dependencies.txt* -> *dependencies.txt*
- *jeka/project-libs* dir -> *libs*


## Major API Change

- KBean classes now are suffixed with `KBean` instead of 'JkBean' (e.g. `ProjectKBean`)
- `JkBean.getBean()`-> `KBean.load()`

## Properties

- `jeka.classpath.inject`-> `jeka.inject.classpath`
-  KBean reference has changed from `myKbean#xxx` to `@myKbean.xxx`

## Command Line

- KBean reference can be used for many invoke and notation has changed for `myKBean#` to `myKBean: `
  
  Example: `project#pack project#tests.skip=true`
  
  Is now :   `project: pack tests.skip=true` 

- Use `jeka --help` for command line help starting point
The `dev.jeka.core.api.tooling` package provides integration with tools that developers generally deal with.

## Eclipse

`JkEclipseClasspathGenerator` and `JkEclipseProjectGenerator` provides a method for generating a proper .classpath and .project file respectively.

`JkEclipseClasspathApplier` reads information from a .classpath file.

## Intellij

`JkIntellijImlGenerator` generates proper .iml files.

## Git

`JkGitWrapper` wraps common Git commands in a lean API.

## Maven

`JkMvn` wraps Maven command line in a lean API

`JkPom` reads POM/BOM to extract information like : declared dependencies, dependency management, repos,
properties, version, and artifactId.



# Sample Gradle Project

A minimal multi-module Gradle project used to test the JeKa Gradle plugin.

## Structure

| Module | Description                                      |
|--------|--------------------------------------------------|
| `lib`  | Library using [Guava](https://github.com/google/guava), with JUnit 5 tests |
| `app`  | Application depending on `lib` and SLF4J         |

## Build with Gradle

```bash
# Build all modules
gradle build

# Build, skipping tests
gradle build -x test

# Run tests
gradle test

# Run the application
gradle :app:run

# Install all modules to local Maven repository (~/.m2)
gradle publishToMavenLocal
```

## Build with JeKa Gradle Plugin

First build the plugin JAR from the `plugins.gradle` directory:

```bash
jeka project: pack
```

Then from this `sample/` directory:

```bash
# Run an arbitrary Gradle command
jeka gradle: wrap --gradle.args="build" --gradle.appModule=app

# Run the application
jeka gradle: wrap --gradle.args=":app:run"

# Build (skipping tests) and copy app artifacts to jeka-output/
jeka gradle: wrapBuild --gradle.appModule=app
```

## Run from Source

This project includes a `jeka.properties` that makes it source-runnable via the JeKa Gradle plugin.
Build and run in one step from this `sample/` directory:

```bash
jeka app: run
```

JeKa will invoke `gradle: wrapBuild appModule=app`, which:
1. Runs `gradle clean build -x test` from the project root
2. Copies the fat JAR from `app/build/libs/` to `jeka-output/`
3. Runs the resulting JAR

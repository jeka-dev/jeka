# Kotlin JVM Plugin for JeKa

A plugin designed for building JVM projects containing Kotlin source code.  

This plugin provides a `KotlinJvmKBean` to automatically configure the *project KBean*.  
It also includes the `JkKotlinJvm` utility for programmatic project setup.

### Resources

- Command-line documentation: `jeka kotlin: --doc`
- Source code: [View Source](src/dev/jeka/plugins/kotlin/KotlinJvmKBean.java).
- Example project: [SpringBoot Kotlin ReactJS Example](https://github.com/jeka-dev/working-examples/tree/master/springboot-kotlin-reactjs).

## Initialization

The plugin customizes the *project KBean* found in the runbase for the following:
- Sets `src/main/kotlin` and `src/test/kotlin` as the directories for project source and test source respectively.
- Adds a compile pre-action to compile Kotlin source code.
- Adds the Kotlin Stdlib to the project dependencies.

Java code can coexist with Kotlin code in the `src/main/kotlin` directory, but Java code in the `src/main/java` directory will be ignored.

The version of Kotlin used is specified in the `jeka.kotlin.version` property, which is also applied when compiling code in the *jeka-src* directory.

## Configuration
No additional configuration is necessary by default. However, the following options can be used to customize settings:

```properties
jeka.inject.classpath=dev.jeka:kotlin-plugin
@kotlin=
# Optional properties
@kotlin.sourceDir=src
@kotlin.testSourceDir=test
@kotlin.includeStdlib=false
```

## Programmatic Usage
Some settings need to be configured programmatically. Below is an example using Kotlin for consistency:

**Configure Kotlin Compiler**

```kotlin
class Build : KBean() {
    override fun init() {
        // Configure Kotlin compiler
        load(KotlinJvmKBean::class.java).kotlinJvm.kotlinCompiler
            .addPlugin("org.jetbrains.kotlin:kotlin-allopen")
            .addPluginOption("org.jetbrains.kotlin.allopen", "preset", "spring")
    }
}
```
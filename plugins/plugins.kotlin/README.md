# Kotlin JVM Plugin for JeKa

Add Kotlin compilation capability to `project` KBean.

Kotlin code is expected to be found in the *src/main/kotlin* directory.

**This KBean post-initializes the following KBeans:**

| Post-initialised KBean | Description                                                                 |
|------------------------|-----------------------------------------------------------------------------|
| ProjectKBean           | Adds Kotlin source compilation and Kotlin standard library to dependencies. |


**This KBean exposes the following fields:**

| Field                      | Description                                                                                       |
|----------------------------|---------------------------------------------------------------------------------------------------|
| kotlinVersion [String]     | Overrides the Kotlin version for compiling and running defined in 'jeka.kotlin.version' property. |
| sourceDir [String]         | Location of Kotlin sources.                                                                       |
| testSourceDir [String]     | Location of Kotlin sources for tests.                                                             |
| includeStdlib [boolean]    | If true, includes standard lib for compiling.                                                     |
| configureProject [boolean] | If true, the project KBean will be automatically configured to use Kotlin.                        |


## Resources

- Command-line documentation: `jeka kotlin: --doc`
- Source code: [View Source](src/dev/jeka/plugins/kotlin/KotlinJvmKBean.java).
- Example project: [SpringBoot Kotlin ReactJS Example](https://github.com/jeka-dev/working-examples/tree/master/springboot-kotlin-reactjs).

## Configuration Example

```properties
jeka.classpath=dev.jeka:kotlin-plugin
@kotlin=on

# Optional properties
@kotlin.sourceDir=src
@kotlin.testSourceDir=test
@kotlin.includeStdlib=false

# Add Compiler plugin passing some options
@kotlin.plugin.0=org.jetbrains.kotlin:kotlin-allopen-compiler-plugin
@kotlin.pluginOption.0.pluginId=org.jetbrains.kotlin.allopen
@kotlin.pluginOption.0.key=preset
@kotlin.pluginOption.0.value=spring

# Or add the equivalent shorthand using predefined 'preset'
@kotlin.preset.0=SPRING
```

## Programmatic Usage
Some settings need to be configured programmatically. Below is an example using Kotlin for consistency:

**Configure Kotlin Compiler**

```kotlin
class Build : KBean() {
    override fun init() {
        // Configure Kotlin compiler
        load(KotlinKBean::class.java).getKotlinJvm().getKotlinCompiler
            .addPlugin("org.jetbrains.kotlin:kotlin-allopen")
            .addPluginOption("org.jetbrains.kotlin.allopen", "preset", "spring")
    }
}
```
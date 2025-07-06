# Kotlin JVM Plugin for JeKa

Add Kotlin compilation capability to `project` KBean.

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

### Configuring for Spring-Boot

To configure a Spring-Boot project compiled with Kotlin, You just need to set the `jeka.properties`file as following:

```properties
jeka.java.version=21
jeka.kotlin.version=2.1.10

jeka.classpath=dev.jeka:kotlin-plugin dev.jeka:springboot-plugin

jeka.kbean.default=project

@springboot=on

@kotlin=on
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

## Generated Doc

**This KBean post-initializes the following KBeans:**

|Post-initialised KBean   |Description  |
|-------|-------------|
|ProjectKBean |Adds Kotlin source compilation and Kotlin standard library to dependencies. |
|IntellijKBean |Generates specific .idea/kotlinc.xml with relevant values. |


**This KBean exposes the following fields:**

|Field  |Description  |
|-------|-------------|
|kotlinVersion [String] |Overrides the Kotlin version for compiling and running defined in 'jeka.kotlin.version' property. |
|sourceDir [String] |Location of Kotlin sources. |
|testSourceDir [String] |Location of Kotlin sources for tests. |
|includeStdlib [boolean] |If true, includes standard lib for compiling. |
|kotlinApiVersion [String] |The Kotlin api version. If null, this will be deduced from Kotlin version. |
|configureProject [boolean] |If true, the project KBean will be automatically configured to use Kotlin. |
|preset.[key] [enum:KotlinKBean$Preset] | |
|pluginOption.[key].pluginId [String] |The compiler plugin id we want to setup. |
|pluginOption.[key].key [String] |Property key to setup. |
|pluginOption.[key].value [String] |Property value to setup. |
|plugin.[key] [String] | |


**This KBean exposes the following methods:**

|Method  |Description  |
|--------|-------------|
|info |Displays info about Kotlin configuration. |

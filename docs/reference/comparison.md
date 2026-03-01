# JeKa for Maven and Gradle Users

If you are coming from Maven or Gradle, this guide will help you map your existing knowledge to JeKa concepts.

## Core Philosophies

| Feature            | Maven            | Gradle              | JeKa                                        |
|:-------------------|:-----------------|:--------------------|:--------------------------------------------|
| **Configuration**  | XML (`pom.xml`)  | Groovy/Kotlin DSL   | Properties (`jeka.properties`) or Java Code |
| **Extensibility**  | Plugins          | Plugins / Tasks     | KBeans (Java classes similar to JavaBeans)  |
| **Build Logic**    | Rigid Lifecycle  | Task Graph          | Method Invocations                          |
| **Dependencies**   | XML Declarations | DSL Declarations    | Text based(`jeka.project.deps`)      |

## Concept Mapping

### Project Structure

| Maven/Gradle               | JeKa                   | Notes                                                        |
|:---------------------------|:-----------------------|:-------------------------------------------------------------|
| `src/main/java`            | `src/main/java`        | Standard Java structure is supported by the `project` KBean. |
| `src/test/java`            | `src/test/java`        | Standard Test structure is supported.                        |
| `build.gradle` / `pom.xml` | `jeka.properties`      | Global project settings and tool versions.                   |
| Custom Task/Plugin         | `jeka-src/Custom.java` | Any Java code in `jeka-src` is part of your build logic.     |

### Common Commands

| Maven               | Gradle                       | JeKa                                  |
|:--------------------|:-----------------------------|:--------------------------------------|
| `mvn compile`       | `gradle compileJava`         | `jeka project: compile`               |
| `mvn test`          | `gradle test`                | `jeka project: test`                  |
| `mvn package`       | `gradle assemble`            | `jeka project: pack`                  |
| `mvn install`       | `gradle publishToMavenLocal` | `jeka maven: publishLocal`            |
| `mvn clean`         | `gradle clean`               | `jeka --clean`  or `jeka -c`          |
| `mvn help:describe` | `gradle help`                | `jeka --doc` or `jeka project: --doc` |

## Key Differences

### 1. Unified Model for Plugins and Build Logic
Whether writing custom build logic or creating a reusable plugin, you use the same simple Java model: KBeans. They are similar to JavaBeans.

### 2. Pure Java Build Logic
Instead of learning a specific DSL (Domain Specific Language) like Gradle's Groovy/Kotlin, you use **standard Java**. If you can write a Java method, you can write a build task.

### 3. Immediate Feedback
JeKa compiles and runs your build logic on-the-fly. There is no "configuring" phase that takes several seconds before the build starts.

### 4. No XML or YAML
Use properties for configuration and Java code for logic.

## Example: Adding a Dependency 

**Maven:**
```xml
<dependency>
  <groupId>com.google.guava</groupId>
  <artifactId>guava</artifactId>
  <version>31.1-jre</version>
</dependency>
<dependency>
  <groupId>org.threeten</groupId>
  <artifactId>threeten-extra</artifactId>
  <version>1.8.0</version>
</dependency>
<dependency>
  <groupId>com.google.guava</groupId>
  <artifactId>guava</artifactId>
  <version>31.1-jre</version>
  <scope>test</scope>
</dependency>
```

**Gradle:**
```kotlin
implementation("com.google.guava:guava:31.1-jre")
implementation("org.threeten:threeten-extra:1.8.0")
test("org.mockito:mockito-core:5.22.0")
```

**JeKa (jeka.project.deps):**
```ini
[compile]
com.google.guava:guava:31.1-jre
org.threeten:threeten-extra:1.8.0

[test]
org.mockito:mockito-core:5.22.0
```



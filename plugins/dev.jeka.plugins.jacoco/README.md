# Jacoco Plugin for JeKa

A plugin to integrate the [Jacoco](https://www.eclemma.org/jacoco) coverage tool with JeKa.

This plugin provides a [KBean](src/dev/jeka/plugins/jacoco/JacocoKBean.java) for automatically configuring the *ProjectKBean*.
It also includes utility classes to programmatically configure projects.

### Resources

- Command-line documentation: `jeka jacoco: --doc`
- Source code: [View the source](src/dev/jeka/plugins/jacoco/JacocoKBean.java)

## Initialization

The plugin automatically detects the presence of a *Project KBean* in the running context.
If found, it configures the project to execute tests with the *Jacoco* agent.
Test coverage reports are generated and stored in the `jeka-output/jacoco` directory.

## Configuration

No manual setup is necessary, as the plugin provides intuitive default settings.
However, you can override certain properties as follows:

```properties
jeka.classpath=dev.jeka:jacoco-plugin
@jacoco=
# Optional properties
@jacoco.jacocoVersion=0.8.7
@jacoco.configureProject=true
@jacoco.htmlReport=true
```

Refer to `java jacoco: --doc` for additional details.

## Programmatic Usage

Use the `JkJacoco` class to configure projects directly, as shown below:

```java
JkProject project = myProject();
JkJacoco.ofVersion("0.8.11")
   .configureForAndApplyTo(project);
```

Once configured, the project runs tests with the *Jacoco* agent.  
To run tests programmatically, use: `JkProject.testing.run()` method.
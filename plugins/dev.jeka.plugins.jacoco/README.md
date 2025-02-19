# Jacoco Plugin for JeKa

A plugin to integrate the [Jacoco](https://www.eclemma.org/jacoco) coverage tool with JeKa.

This plugin provides a [KBean](src/dev/jeka/plugins/jacoco/JacocoKBean.java) for automatically configuring the *ProjectKBean*.
It also includes utility classes to programmatically configure projects.

**This KBean post-initializes the following KBeans:**

|Post-initialised KBean   |Description  |
|-------|-------------|
|ProjectKBean |Appends a Jacoco agent to the process running tests. |


**This KBean exposes the following fields:**

|Field  |Description  |
|-------|-------------|
|configureProject [boolean] |If true, project from ProjectJkBean will be configured with Jacoco automatically. |
|xmlReport [boolean] |If true, Jacoco will produce a standard XML report usable by Sonarqube. |
|htmlReport [boolean] |If true, Jacoco will produce a standard HTML report . |
|agentOptions [String] |Options string, as '[option1]=[value1],[option2]=[value2]', to pass to agent as described here : https://www.jacoco.org/jacoco/trunk/doc/agent.html. |
|classDirExcludes [String] |Exclusion patterns separated with ',' to exclude some class files from the XML report input. An example is 'META-INF/**/*.jar'. |
|jacocoVersion [String] |Version of Jacoco to use both for agent and report. The version will be resolved against coordinate 'org.jacoco:org.jacoco.agent'. |


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
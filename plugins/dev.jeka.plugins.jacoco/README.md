# Jacoco Plugin for JeKa

Plugin to use the [Jacoco](https://www.eclemma.org/jacoco) coverage tool with JeKa.

This plugin contains a [KBean](src/dev/jeka/plugins/jacoco/JacocoKBean.java) to auto-configure *ProjectKBean*.

It also contains utilities class to configure projects programmatically.

## Configure using Kean

```properties
jeka.classpath.inject=dev.jeka:jacoco-plugin

# Instantiate jacoco KBean
jeka.cmd._append=jacoco#

# Optional settings. Execute `jeka jacoco#help` to see available options.
jacoco#jacocoVersion=0.8.7
```

## Configure Programmatically

You can use directly `JkJacoco` in build code to achieve lower level actions.

```java
@Override
private void init() {
    JkProject project = projectKBean.project;
    JkJacoco.ofVersion("0.8.11")
            .configureForAndApplyTo(project);
}
```

The [JkJacoco class](src/dev/jeka/plugins/jacoco/JkJacoco.java) also provides fine grained methods to deal with 
various *Jacoco* use cases.

### Example

See example [here](../../samples/dev.jeka.samples.jacoco)


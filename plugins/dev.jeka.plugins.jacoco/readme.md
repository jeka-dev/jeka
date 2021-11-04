![Build Status](https://github.com/jerkar/jacoco-plugin/actions/workflows/push-master.yml/badge.svg)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/dev.jeka/jacoco-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/dev.jeka/protobuf-plugin) <br/>

# Jeka library/plugin for Jacoco

Plugin to use the [Jacoco](https://www.eclemma.org/jacoco) coverage tool in your Java builds

## How to use

Just declare the plugin in your build class.  

```java
@JkDefClasspath("dev.jeka:jacoco-plugin")
public class Build extends JkClass {
    
    JkPluginJava javaPlugin = getPlugin(JkPluginJava.class);
    
    JkPluginJacoco jacoco = getPlugin(JkPluginJacoco.class);

    ...
}
```
The plugin will configure java project in such tests are launched with jacoco agent. 
Jacoco reports are output in output/jacoco dir.

### Programmatically

You can use directly `JkJacoco` in build code to perform lower level actions.

### Bind Jacoco dynamically

You can invoke Jacoco plugin from command line on a Jeka project that does declare this plugin in its build class.

`jeka @dev.jeka:jacoco-plugin jacoco# java#pack`

To get help and options :
`jeka jacoco#help`
`jeka @dev.jeka:jacoco-plugin jacoco#help`

### Example

See example [here](../../samples/dev.jeka.samples.jacoco)


# NodeJs Plugin for JeKa

This Plugin provides a [KBean](src/dev/jeka/plugins/nodejs/NodeJsKBean.java) to auto-configure projects
having nodeJs client.

It also provides a library to install and invoke conveniently NodeJs from Java.


## Configure using KBeans

```properties
jeka.classpath.inject=dev.jeka:nodejs-plugin

# Load the plugin in the runbase.
jeka.cmd._append=nodeJs#

# Optional tool settings, visit KBean source code for exhaustive options
nodeJs#version=18.19.0
```

## Configure Programmatically

Just declare the plugin in your build class, as follows :

```java
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.tool.JkDoc;

@JkDefClasspath("dev.jeka:nodejs-plugin")
public class Build extends KBean {

    @Override
    protected void init() {
        JkSonarqube.ofVersion("5.0.1.3006")
                .setProperties(getRuntime().getProperties())  // Take Sonar properties from local.properties and System.getProperties()
                .configureFor(project)
                .run();
    }
}
```
This will configure a Sonarqube KBean according the specified *project*.

The analysis must be triggered explicitly, by invoking the `JkSonarqube#run` method.

## Example

See example [here](../../samples/dev.jeka.samples.sonarqube)






This plugin provides :
- a KBean to automatically download/install the specified version of NodeJs
- A utility class to invoke NodeJs tool in a platform independent manner.

To use in your Kbean, import it using `@JkImportClasspath("dev.jeka:nodejs-plugin")` 

Then you can define the NodeJs version to use and invoke *npm* and other commands from `NodeJsJkBean`.

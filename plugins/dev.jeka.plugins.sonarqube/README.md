# Sonarqube Plugin for JeKa

This plugin provides utility classes and KBean to perform Sonarqube code analysis.

## Configure using KBeans

[SonarqubeKBean](src/dev/jeka/plugins/sonarqube/SonarqubeKBean.java) configures itself from KBeans found 
in the runtime and specified options.

The *SonarqubeKBean* does not launch automatically. The method `SonarqubeKBean#run` should bbe explicitly invoked.

```properties
jeka.classpath.inject=dev.jeka:sonarqube-plugin

# Optional : define a command short-cut to build a project and launch Sonarqube analysis in a row.
jeka.cmd.buildQuality=project#cleanPack sonarqube#run

# Optional tool settings
sonarqube#scannerVersion=5.0.1.3006

# all properties starting with 'sonar.' will be injected as is in the <i>Sonarqube</i> command line.
sonar.host.url=http://localhost:9000
```

## Configure Programmatically

Just declare the plugin in your build class, as follows :

```java
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.tool.JkDoc;

@JkDefClasspath("dev.jeka:sonarqube-plugin")
public class Build extends KBean {

    @JkDoc("Run Sonarqube analysis.")
    public void runSonarqube() {
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




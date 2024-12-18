# Sonarqube Plugin for JeKa

This plugin provides utility classes and KBean to perform a SonarQube code analysis.
A SonarScanner is actually passed on the code analysis and sent to a Sonar server.

Resources:
  - Command-line documentation: `jeka sonarqube: --doc`
  - Source Code: [Visit here](src/dev/jeka/plugins/sonarqube/SonarqubeKBean.java).
  - SonarQube: [Vis-sit here](https://www.sonarsource.com/fr/products/sonarqube/).
  - Sonarqube properties: [Visit here](https://docs.sonarsource.com/sonarqube-server/10.6/analyzing-source-code/analysis-parameters/).

## Initialization

Nothing special happens at initialization time, the plugin just configure itself from information taken from 
*project KBean*. The plugin does not need to be initialized explictly as it will be implictly when `run`method 
will be invoked.

As no post-action is registered, run a Sonar analysis by  executing `jeka sonarqube: run`.

## Configuration

No configuration is requires. Nevertheless, the plugin offers some settings via properties. 
Sonar scanner properties can be passed using System properties or *jeka.properties* file.

```properties
jeka.classpath.inject=dev.jeka:sonarqube-plugin

# Optional properties
@sonarqube.scannerVersion=5.0.1.3006
@sonarqube.logOutput=false
@sonarqube.pingServer=false

# Sonar properties passed to SonerScanner
sonar.host.url=http://my.sonar.server:9000
sonar.projectDescription=A demo project for showcasing JeKa.
```

Properties:
  - scannerVersion: Force to run the specified version of Sonar Scanner.
  - logOutput: Log the scanner output on the console  (default: true).
  - pingServer: Ping Sonarqube server for having comprehensible error message when the server is not reachable.

All properties in `jeka.properties` starting with `sonar.` are taken in account by the Sonar scanner.
It is also possible to pass system props as `jeka sonatqube: run -Dsonar.token=Xxxxxxxx`.

## Programmatic Usage

We can ru Sonar scanner programmatically as below:

```java
JkProject project = myProject();
JkSonarqube.ofVersion("5.0.1.3006")
        .setProperties(getRuntime().getProperties())  // Take Sonar properties from local.properties and System.getProperties()
        .configureFor(project)
        .run();
    }
}
```

### Advanced scenario

We might analysis projects containing both Java and Javascript code. 
This is possible using the programmatic configuration, as used in [this example](https://github.com/jeka-dev/demo-build-templates/blob/b0b3940068bc96a02c9f4e2e46766355466b1df4/jeka-src/dev/jeka/demo/templates/SpringBootTemplateBuild.java#L109).







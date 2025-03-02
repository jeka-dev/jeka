# Sonarqube Plugin for JeKa
Runs Sonarqube analysis and checks quality gates.

The properties prefixed with 'sonar.', such as '-Dsonar.host.url=http://myserver/..', will be appended to the SonarQube configuration.


**This KBean post-initializes the following KBeans:**

| Post-initialised KBean | Description                                  |
|------------------------|----------------------------------------------|
| ProjectKBean           | Adds Sonarqube analysis to quality checkers. |


**This KBean exposes the following fields:**

| Field                           | Description                                                                                                                                                                                                                                                                      |
|---------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| provideProductionLibs [boolean] | If true, the list of production dependency files will be provided to sonarqube.                                                                                                                                                                                                  |
| provideTestLibs [boolean]       | If true, the list of test dependency files will be provided to sonarqube.                                                                                                                                                                                                        |
| scannerVersion [String]         | Version of the SonarQube client to run. It can be '+' for the latest one (at the price of a greater process time). The version will be resolved against 'org.sonarsource.scanner.cli:sonar-scanner-cli' coordinate. Use a blank string to use the client embedded in the plugin. |
| logOutput [boolean]             | If true, displays sonarqube output on console.                                                                                                                                                                                                                                   |
| pingServer [boolean]            | Ping the sonarqube server prior running analysis.                                                                                                                                                                                                                                |
| gate [boolean]                  | If true, the quality gate will be registered alongside analysis in project quality checkers.                                                                                                                                                                                     |


**This KBean exposes the following methods:**

| Method   | Description                                                                                                          |
|----------|----------------------------------------------------------------------------------------------------------------------|
| check    | Checks if the analysed project passes its quality gates. The 'run' method is expected to have already been executed. |
| run      | Runs a SonarQube analysis and sends the results to a Sonar server.                                                   |


Resources:
  - Source Code: [Visit here](src/dev/jeka/plugins/sonarqube/SonarqubeKBean.java).
  - SonarQube: [Visit here](https://www.sonarsource.com/fr/products/sonarqube/).
  - Sonarqube properties: [Visit here](https://docs.sonarsource.com/sonarqube-server/10.6/analyzing-source-code/analysis-parameters/).

## Configuration Example

```properties
jeka.classpath=dev.jeka:sonarqube-plugin

# Optional properties
@sonarqube.scannerVersion=5.0.1.3006
@sonarqube.logOutput=false
@sonarqube.pingServer=false

# Sonar properties passed to SonerScanner
sonar.host.url=http://my.sonar.server:9000
sonar.projectDescription=A demo project for showcasing JeKa.
```

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

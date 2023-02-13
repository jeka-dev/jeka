![Build Status](https://github.com/jerkar/sonarqube-plugin/actions/workflows/push-master.yml/badge.svg)
[![Maven Central](https://img.shields.io/maven-central/v/dev.jeka/sonarqube-plugin.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22dev.jeka22%20AND%20a:%22springboot-plugin%22)

# Sonarqube Plugin for Jeka

This plugin provides utility classes and KBean to perform Sonarqube code analysis.

## How to Use Programmatically

Just declare the plugin in your build class, as follows :

```java
@JkDefClasspath("dev.jeka:sonarqube-plugin")
public class Build extends JkClass {

    SonarqubeJkBean sonarqube = getPlugin(SonarqubeJkBean.class);
    ...
```
This will configure a Sonarqube KBean according the *Project KBean* declared in your build class.

The `sonarqube`instance can be customized in your programmatically build class in your build 
class or by passing standard *sonarqube* properties, such as `sonar.host.url`.

The analysis must be triggered explicitly, by invoking the `run` method on the `sonarqube` KBean.

## How to Use Dynamically

The sonarqube KBean can be invoked dynamically (you don't need to declare it in your build class)
```
./jekaw @dev.jeka.plugins:sonarqube sonarqube#run -Dsonar.host.url=https://my.sonar/server/url
```

You can store this setting in your *local.properties* file, as follows:
```properties
jeka.cmd._append=@dev.jeka:sonarqube-plugin
jeka.cmd.sonar=sonarqube#run
jeka.cmd.build=project#pack :sonar

sonar.host.url=https://my.sonar/server/url
sonar.login=mylogin
sonar.password=mypassword
sonar.inclusions=...
```
Executing `jeka :build` will perform a project *pack* followed by a *Sonarque* analysis.

See available sonar option [here](https://docs.sonarqube.org/latest/analysis/analysis-parameters/).

### Example

See example [here](../../samples/dev.jeka.samples.sonarqube)




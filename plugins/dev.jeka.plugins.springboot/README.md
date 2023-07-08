![Build Status](https://github.com/jerkar/springboot-plugin/actions/workflows/push-master.yml/badge.svg)
[![Maven Central](https://img.shields.io/maven-central/v/dev.jeka/springboot-plugin.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22dev.jeka22%20AND%20a:%22springboot-plugin%22)

# Springboot plugin for Jeka

[Jeka](https://jeka.dev) plugin to build Spring Boot applications with minimal effort. <br/>

## Create a new Springboot project

To create a new project from scratch, execute :
Execute command line : 
```shell
jeka scaffold#run scaffold#wrapper @dev.jeka:springboot-plugin springboot#
``` 
!!!
  Note: you need to have installed JeKa on your machine to execute this command line.
  Alternatively, Jeka Ide for IntelliJ does not required to have Jeka installed£


## Configure declaratively 

Add the following in your *local.properties* file

```properties
jeka.cmd._append=@dev.jeka:springboot-plugin springboot#
springboot#springbootVersion=3.0.1
```
This will add *Springboot Plugin* to *Jeka* classpath, and instantiate *springboot* KBean.
This has the effect to modify the *project* in such this produces a bootable jar or 
deployable *war* archive.

To add needed dependencies, edit the *project-dependencies.txt* file as follows :
```text
==== COMPILE ====
org.springframework.boot:spring-boot-starter-web

==== TEST ====
org.springframework.boot:spring-boot-starter-test
```

To display available options, execute :
```shell
./jekaw springboot#help
```

## Configure with build code

Just declare the plugin in your Jeka class (in _[project Dir]/jeka/def_ ) as above :

```java
@JkInjectClasspath("dev.jeka:springboot-plugin")
class Build extends JkBean {

    SpringbootJkBean springbootBean = getJkBean(SpringbootJkBean.class);

    Build() {
        springbootBean.springbootVersion = "3.0.5";
        springbootBean.projectBean.lately(this::configure);
    }

    private void configure(JkProject project) {
        project.flatFacade()
                .configureCompileDependencies(deps -> deps
                        .and(Boot.STARTER_WEB)
                )
                .configureTestDependencies(deps -> deps
                        .and(Boot.STARTER_TEST)
                );
    }

}
```
Note : 
  You don't need to repeat what has been configured declaratively. If you have declared, springboot plugin 
  and dependencies in *local.properties* and *project-dependencies.txt*, you don't need to repeat it in the build code.

Execute`jeka project#pack` to make the bootable jar file. This command processes :
* Compilation and tests
* Generation of the original binary jar along its sources jar
* Generation of the bootable jar

Springboot plugin also provides convenient class constants to declare usual dependencies used in springboot projects.

```java
    ...
    .configureCompileDeps(deps -> deps
        .and(Boot.STARTER_WEB)
        .and(Boot.STARTER_TEST, JkJavaDepScopes.TEST)
        .and(Fwk.JDBC)
        .and(Data.MONGODB)
        .and(Data.COMMONS)
        .and(securityOn, Boot.STARTER_SECURITY)    		  
    );    
}
```

## How does it work ?

This plugin contains a KBean (`SpringbootJkBean`) that :
  * Reads the Springboot pom/bom for the specified version. 
    * Configure the _project_ KBean (`ProjectJkBean`) such as :
        * The springboot _bom_ is added to project dependencies
        * The produced jar is a bootable jar.

Utility methods are also provided if you want to construct your own springboot jar and configure dependencies without 
embracing the plugin mechanism.

 


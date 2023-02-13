![Build Status](https://github.com/jerkar/springboot-plugin/actions/workflows/push-master.yml/badge.svg)
[![Maven Central](https://img.shields.io/maven-central/v/dev.jeka/springboot-plugin.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22dev.jeka22%20AND%20a:%22springboot-plugin%22)

# Springboot plugin for Jeka

[Jeka](https://jeka.dev) plugin to build Spring Boot applications with minimal effort. <br/>

## Scaffold a Springboot project

Execute command line : `jeka scaffold#run scaffold#wrapper @dev.jeka:springboot-plugin springboot#` 

In Jeka IDE, you can just __right-click__ on module root folder, then __scaffold... | Springboot__

## How to Use declaratively 

Add the following in your *local.properties* file

```properties
jeka.cmd._append=@dev.jeka:jacoco-plugin springboot#
springboot#springbootVersion=3.0.1
```
This will add *Springboot Plugin* to *Jeka* classpath, and instantiate *springboot* KBean.
This has the effect to modify the *project* in such this produces a bootable jar or 
deployable *war* archive.

To add needed dependencies, edit the *dependencies.txt* file as follows :
```text
==== COMPILE ====
org.springframework.boot:spring-boot-starter-web

==== TEST ====
org.springframework.boot:spring-boot-starter-test
```

## Writing the build class manually

Just declare the plugin in your Jeka class (in _[project Dir]/jeka/def_ ) as above :

```java
@JkInjectClasspath("dev.jeka:springboot-plugin")
class Build extends JkBean {

    SpringbootJkBean springbootBean = getJkBean(SpringbootJkBean.class);

    Build() {
        springbootBean.springbootVersion = "3.0.1";
        springbootBean.projectBean().configure(this::configure);
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


Running the main method or executing `jeka project#pack` performs :

* Compilation and tests run
* Generation of the original binary jar along its sources jar
* Generation of the executable jar

This plugin reads the Springboot pom/bom for the specified version and enrich the _project_ plugin with dependency version provider according the pom. It also instructs java plugin to produce a workable springboot jar instead of the vanilla jar. 

Utility methods are provided if you want to construct your own springboot jar and dependency version provider without embracing the plugin mechanism.

### Adding extra dependencies
 
Springboot plugin provides class constants to declare usual dependencies used in springboot projects. 
It adds great comfort when picking some Spring dependencies.
 
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

![Build Status](https://github.com/jerkar/springboot-plugin/actions/workflows/push-master.yml/badge.svg)
[![Maven Central](https://img.shields.io/maven-central/v/dev.jeka/springboot-plugin.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22dev.jeka22%20AND%20a:%22springboot-plugin%22)

# Spring-Boot plugin for Jeka

[Jeka](https://jeka.dev) plugin to build Spring-Boot applications with minimal effort. <br/>

This plugin contains a [KBean](src/dev/jeka/plugins/springboot/SpringbootKBean.java) and a library to handle the build of Spring-Boot applications, and specially 
the creation of a bootable jar.

## Create a new Spring-Boot Project from Scratch

To create a new project from scratch, execute :

```shell
jeka scaffold#run scaffold#wrapper @dev.jeka:springboot-plugin springboot#
``` 
This will download this plugin, and scaffold a Spring-Boot project for you in the working dir.
!!!
  Note: you need to have installed JeKa on your machine to execute this command line.
  Alternatively, Jeka Ide for IntelliJ does not required to have Jeka installed.


## Configure a Project using Spring-Boot KBean 

[SpringbootKBean](src/dev/jeka/plugins/springboot/SpringbootKBean.java) is designed to auto-configure
*ProjectKBean*, *SelfAppKBean* and *ScaffoldKBean* when present in runtime.

It configures following KBeans when it is loaded in the runtime :
- `ProjectKBean` : 
  - Adds Spring-Boot bom to dependencies of the project.
  - Instructs project to create bootable jar. War file and original artifacts can be generated as well
  - Sets sensitive default as not producing javadoc or source artifacts.
- `SelfAppKBean` : Instructs app to create bootable jar.
- `ScaffoldKBean` : Creates specific build class and simple Spring-Boot app template code.

### Instantiate Spring-Boot KBean

As all KBean, this can be instantiated both using *property file* or *programmatically*.

Using Properties:
```properties
jeka.cmd._append=@dev.jeka:springboot-plugin springboot#
```

Programmatically:

```java
import dev.jeka.plugins.springboot.SpringbootKBean;

@JkInjectClasspath("dev.jeka:springboot-plugin")
class MyBuild extends KBean {

    MyBuild() {
      load(SpringbootKBean.class);  // initializes the KBean and configures related KBeans (see above)
    }
  
}
```

### Configuration

This KBean can only be configured by properties.


```properties
jeka.cmd._append=@dev.jeka:springboot-plugin springboot#
springboot#springbootVersion=3.0.1
```
jeka.cmd._append=@dev.jeka:springboot-plugin springboot#
springboot#springbootVersion=3.0.1

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

 


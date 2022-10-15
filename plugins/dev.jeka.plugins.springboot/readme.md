![Build Status](https://github.com/jerkar/springboot-plugin/actions/workflows/push-master.yml/badge.svg)
[![Maven Central](https://img.shields.io/maven-central/v/dev.jeka/springboot-plugin.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22dev.jeka22%20AND%20a:%22springboot-plugin%22)

# Springboot plugin for Jeka

[Jeka](https://jeka.dev) plugin to build Spring Boot applications with minimal effort. <br/>

## Scaffold a Springboot project

Execute command line : `jeka scaffold#run scaffold#wrapper @dev.jeka:springboot-plugin springboot#` 

In Jeka IDE, you can just __right-click__ on module root folder, then __scaffold... | Springboot__

## Writing the build class manually

Just declare the plugin in your Jeka class (in _[project Dir]/jeka/def_ ) as above :

```java

import dev.jeka.core.api.depmanagement.JkJavaDepScopes;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkInjectClasspath;
import dev.jeka.plugins.springboot.SpringbootJkBean;

import static dev.jeka.core.plugins.springboot.JkSpringModules.Boot;

@JkInjectClasspath("dev.jeka:springboot-plugin")
class Build extends JkBean {

    SpringbootJkBean springbootBean = getJkBean(SpringbootJkBean.class);

    Build() {
        springbootBean.springbootVersion = "2.5.5";
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

## Scaffold a springboot project for jeka

* Execute `jeka scaffold#run springboot# @dev.jeka:springboot-plugin`

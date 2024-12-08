# Under The Hood

Jeka consists of a single zero-dependency JAR file, along with two shell scripts (one Powershell script for Windows and one Bash script for Linux/macOS).

## Inside the Jeka JAR

The Jeka JAR is structured as follows:

- `dev.jeka.core.tool` package:

    Contains classes for running Java externally, including the `dev.jeka.core.tool.Main` class and the component model.

- `dev.jeka.core.tool.builtin` package: 

     Contains *KBeans* bundled with JeKa, as `ProjectKBean`, `DockerKBean` or `NativeKBean`.

- `dev.jeka.core.api` package:

  Includes libraries for building projects. These classes can be easily used outside Jeka and embedded in your product.    

```mermaid
graph TD
    subgraph dev.jeka.core
        subgraph tool 
            engine[dev.jeka.core.tool]
            builtin[dev.jeka.core.tool.builtin]
        end
    
        subgraph api
            project[dev.jeka.core.api.project]
            java[dev.jeka.core.api.java]
            system[dev.jeka.core.api.system]
            dep[dev.jeka.core.api.depmanagement]
            file[dev.jeka.core.api.file]
            utils[def.jeka.core.api.utils]
            misc[...]
        end
        
        
    end

    engine --> java
    engine --> system
    engine --> dep
    builtin --> engine
    builtin --> api
    java --> utils
    dep --> utils
    file --> utils
    system --> utils
```
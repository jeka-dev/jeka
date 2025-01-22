# Project KBean

<!-- autogen-doc -->


[`ProjectKBean`](https://github.com/jeka-dev/jeka/blob/master/dev.jeka.core/src/main/java/dev/jeka/core/tool/builtins/project/ProjectKBean.java) 
acts as a wrapper around a [`JkProject`](api-project.md) to facilitate the building of JVM-based code hosted in a project structure.
This _KBean_ provides core methods for fundamental build tasks, including **compiling**, **testing**, and **packaging**.

To work effectively with this KBean, it's helpful to have an [overview](api-project.md) of the capabilities offered by the `JkProject` object.

**Key Features**

- Resolves dependencies, compiles code, and runs tests.
- Creates various types of JAR files out-of-the-box, including regular, fat, shaded, source, and Javadoc JARs.
- Infers project versions from Git metadata.
- Executes packaged JARs.
- Displays dependency trees and project setups.
- Scaffolds skeletons for new projects.

Additionally, `ProjectKBean` serves as a central point of interaction for other KBeans, enabling them to access project details and extend or enhance the build process.

**Example for getting information about source files:**
```Java
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.tool.builtins.project.ProjectKBean;

class MyBuild extends KBean {

    final JkProject project = load(ProjectKBean.class).project;
    
    private List<Path> allSourceFiles;
    
    protected void init() {
        allSourceFiles = project.compilation.layout.resolveSources().getFiles();
        ...
    }
}
```

**Example taken from JeKa:**

- [Jacoco KBean](https://github.com/jeka-dev/jeka/blob/master/plugins/dev.jeka.plugins.jacoco/src/dev/jeka/plugins/jacoco/JacocoKBean.java): 
A KBean that reads te underlying `JkProject` and modifies its testing behavior.
- [Sonarqube KBean](https://github.com/jeka-dev/jeka/blob/master/plugins/dev.jeka.plugins.sonarqube/src/dev/jeka/plugins/sonarqube/SonarqubeKBean.java):
A KBean that reads te underlying `JkProject` to extract information.
- [Protobuf KBean](https://github.com/jeka-dev/jeka/blob/master/plugins/dev.jeka.plugins.protobuf/src/dev/jeka/plugins/protobuf/ProtobufKBean.java):
  A KBean that adds a Proto-buffer code generation to the underlying `JkProject`.


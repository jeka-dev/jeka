# Project KBean

<!-- header-autogen-doc -->


[`ProjectKBean`](https://github.com/jeka-dev/jeka/blob/master/core/src/main/java/dev/jeka/core/tool/builtins/project/ProjectKBean.java) 
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

It offers standardized methods that cover the whole build life-cycle:

- `scaffold`: Creates new project structure from scratch
- `generateSources`: Generates source code
- `compile`: Compiles source code
- `test`: Compiles and run test code
- `pack`: Creates packaged artifacts as JAR files
- `checkQuality`: Runs quality checkers and quality gates
- `e2eTest`: Runs end-to-end test on a deployed version of the application

The `JkProject` instance offers methods to customize or extend behavior, allowing seamless integration of third-party extensions.

**Example for getting information about source files:**

```Java
class MyBuild extends KBean {

  private List<Path> allSourceFiles;

  @JkPostInit
  private void postInit(ProjectKBean projectKBean) {
      allSourceFiles = projectKBean.project.compilation.layout.resolveSources().getFiles();
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

## Annotation Processors

To use an annotation processor (like **Lombok** or **MapStruct**), add the dependency coordinates
to the `compile-only` section in your `jeka.properties` file.


```ini
[compile-only]
org.mapstruct:mapstruct-processor:1.6.3
org.projectlombok:lombok:1.18.38
```

If some annotation processors generate sources, you must mention it in the *jeka.properties* file as:
```properties
@project.compilation.annotationProcSrcGen=true
```
The sources will be generated in *jeka-output/generated-sources/annotation-processors* dir.

That's it!


<!-- body-autogen-doc -->
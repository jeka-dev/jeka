Jeka provides powerful and easy means to reuse build elements across projects.

Elements can be reused as sources in a multi-project structure or exported as binaries in 
order to be used as third-party dependencies to create a plugin ecosystem.

## From Sources

In a multi-project (aka multi-module project), it is possible to use *def classes* defined in other sub-projects.
When using `@JkInjectProject`, classes defined in _../sub-project-1/jeka/def_ and 
in the classpath of _sub-project-1_.   

```java
import dev.jeka.core.tool.JkBean;

@JkInjectProject("../sub-project")
class MyJkBean extends JkBean {
    
}
```

In a _KBean_, it is possible to use a _KBean_ coming from another project.

```java
import dev.jeka.core.tool.JkBean;

class MyJkBean extends JkBean {
    
    @JkInjectProject("../sub-project")
    private OtherJkBean importedBean;
}
```

## From Binaries

To extend Jeka capabilities, it's possible to create a jar file to be used in any _Jeka_ project.
An extension (or plugin) can contain _KBean_ or not, and can have many purposes (such as integrating a specific technology, 
predefining a set of dependencies, providing utiliy classes, etc.).

To achieve this, we must create a project that packs and exports the library.

The project may declare dependencies on Jeka : the simplest way is to add a dependency on the jeka jar that actually builds the project using `JkLocator.getJekaJarPath()`.

```java
import dev.jeka.core.api.project.JkProject;

class Build extends JkBean {

    ProjectJkBean projectBean = getBean(ProjectJkean.class).configure(this::configure);
    
    private void configure(JkProject project) {
        
        // Optional indication about Jeka version compatibility
        JkJekaVersionCompatibilityChecker.setCompatibilityRange(project.getConstruction().getManifest(),
                "0.9.20.RC2",
                "https://raw.githubusercontent.com/jerkar/protobuf-plugin/breaking_versions.txt");
        
        project.simpleFacade()
                .configureCompileDeps(deps -> deps
                        .andFiles(JkLocator.getJekaJarPath())
                );
    }

}
```

### Check Jeka Version Compatibility

`JkJekaVersionCompatibilityChecker.setCompatibilityRange` insert information about Jeka
version compatibility within the Manifest. This information will be used by Jeka to
alert you if the library is marked as being incompatible with the running Jeka version.

The method contains three arguments :

* The object standing for the Manifest file
* The lowest version of Jeka which is compatible with the library
* A url string pointing on a file mentioning the versions of Jeka that are no longer compatible
  with the version of the library

For the last of these, the information has to be stored outside the library itself as the author cannot guess which future version of Jeka will break the compatibility.

An example of such a file is available [here](https://github.com/jerkar/protobuf-plugin/blob/master/breaking_versions.txt)


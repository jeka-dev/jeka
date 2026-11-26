# Working with Multi-Module Projects in JeKa

 Multi-module support in JeKa allows developers to manage and build complex projects with multiple modules in a clean 
 and effective manner.

---

## Structure of a Multi-Module Project

A multi-module project in JeKa consists of:

- A **parent directory** that contains the configuration and dependencies shared across all modules, and declares the location of **child modules**.
- Multiple **child modules**, which are independent or interdependent within the project.

Example:
```
project-root
├── jeka.properties
├── jeka-src (optional)
├── module-1
│   ├── jeka.properties
│   ├── jeka.project.deps
│   ├── src
│   │   ├── main
│   │   └── test
│   └── ...
├── module-2
│   ├── jeka.properties
│   ├── jeka.project.deps
│   ├── src
│   │   ├── main
│   │   └── test
│   └── ...
└── module-Xxxx ...
```
To be recognized as a parent project, the *jeka.properties* file must specify the `_jeka.child-bases` property 
indicating where the child modules are located.

You can set it as `_jeka.child-bases=module1, module2, ...` or `_jeka.child-bases=*`.  
If you use `*`, Jeka will scan child directories and include any with a Jeka structure.

It's also possible to use hierarchical structures such as:
```
parent-project
├── jeka.properties
├── jeka-src (optional)
├── core
│   ├── jeka.properties
│   ├── jeka.project.deps
│   ├── src
│   │   ├── main
│   │   └── test
│   └── ...
└─── plugins
     ├── jeka.properties
     ├── jeka.project.deps
     ├── plugin-common
     │   ├── jeka.project.deps
     │   ├── jeka.properties
     │   └── src
     ├── plugin-1
     │   ├── jeka.project.deps
     │   ├── jeka.properties
     │   └── src
     ├── plugin-2
     │   ├── jeka.project.deps
     │   ├── jeka.properties
     │   └── src
     └── ...
```

!!! warning
    Each intermediate directory must contain a *jeka.properties* file, even empty, as *plugins/jeka.properties* here. 
    Otherwise, inheritance stops there: *plugin-1* would get no property from the root, and would be published without the right version.

### Example of *jeka.properties* for parent project

```properties
# Define Java version for all modules
jeka.java.version=21

# specify location of child modules
_jeka.child-bases=core, plugins/*

# Parent module does not contain a JVM project
_@project=off

# Common settings for child modules containing a JVM project
@project.compilation.compilerOptions=-g
@project.pack.javadocOptions=-notimestamp

# Enable Git-based versioning for Maven (needs a Git repository with at least one commit)
@maven.pub.gitVersioning.enable=true

# Parent module Maven-specific settings: publish a BOM listing the child modules
_@maven.pub.moduleId=dev.jeka:bom
_@maven.pub.parentBom=true

```
Child modules inherit from the properties defined in their parent directory's *jeka.properties* files recursively.

To make a property non-transmitted to the children, we need to prefix it with `_`.

### Typical *jeka.properties* for child module

```properties
# The child module defines a JVM project with specific settings
@project=on
@project.pack.mainClass=dev.jeka.core.tool.Main
+@project.compilation.compilerOptions=-Xlint:none

# Module ID, required in every child when the parent publishes a BOM
@project.moduleId=dev.jeka:jeka-core
```

The child modules inherit properties from the parent, except for properties prefixed with `_`

When a property declaration is prefixed with `+`, the specified value is appended to the parent value instead of overriding it.
In this example, `-g -Xlint:none` will be passed to the compiler options.

## Running JeKa commands 

When executing a JeKa command on a parent project, the specified actions are delegated to all child modules, then to the parent one.

If a module is not concerned with a given KBean, it should declare it in its `jeka.properties` file, as `@project=off`.

If we run the following command on the above example:

```shell
jeka project: pack maven: publish
```
This will first run the `project: pack` method on all child modules. Parent module won't execute these actions 
because it disables the `project` KBean (`_@project=off`).

Then the `maven: publish` action is invoked on all child modules and the parent module as it configures this KBean explicitly (`_@maven.pub.parentBom=true`).

If a child module is not concerned by a specific KBean, it should be explicitly disabled (e.g. `@maven=off`).
This could be the case if *plugin-common* should not be published as a Maven artifact.

### Running child module

If you need to run some Jeka command for a child module, you have 2 options:

1. Use `-cb` (alias `--child-base`) option
   ```shell
   jeka -cb=plugins/plugin-1 project: test pack
   ```
   The value is a path relative to the parent, with `/` or `\` on any OS. If it is not a declared child base,
   the command fails; add `--force` to only get a warning.
   The command applies to the KBeans of the child, as when run from its directory, including KBeans
   defined only in its `jeka-src`.

2. Execute actions from child module directory:
   ```shell
   cd plugins/plugin-1
   jeka project: test pack
   ```
   This approach works well, but code in `jeka-src` declared in parent module won't be taken into account.

## Managing jeka.project.deps

In a multi-module project, we often need to define centrally the versions of the libraries we want to use across all
the modules.

Example:
```ini title="plugins/jeka.project.deps"
[version]
com.google.guava:guava:33.4.7-jre
org.projectlombok:lombok:1.18.38
org.junit:junit-bom:5.12.2@pom  # Use versions defined in this BOM
```

Child module `jeka.project.deps` files inherit the `version` section from the parent directory's `jeka.project.deps`, if present.

Module interdependency can be defined using the module's relative path.

```ini title="plugins/plugin-1/jeka.project.deps"
[compile]
../plugin-common 
com.google.guava:guava

[compile-only]
org.projectlombok:lombok

[processor]
org.projectlombok:lombok

[test]
org.junit.jupiter:junit-jupiter
org.junit.platform:junit-platform-launcher
```

## Programmatic Approach

By combining multi-module and programmatic approach, you can:

- Use `jeka-src` code from one module in another module that depends on it.
- Import a run base (or any `KBean`) from one module into another.

### Using `jeka-src` Code from Another Module

You may need to share build code between multiple modules.  
This is easy to do using the `@JkDep` annotation, which lets you import code from another module.

**Example:**

Let’s say there’s a central set of dependencies in the `jeka-src` folder of the `plugin-common` module.  
These dependencies can be defined as constants in a class, like a `PluginCommon` class, so they can be reused in other modules.

```java title="plugins/plugin-common/jeka-src/PluginCommon.java"
import dev.jeka.core.api.depmanagement.JkDependencySet;

class PluginCommon {

    static final JkDependencySet LOG_LIBS = JkDependencySet.of()
            .and("ch.qos.logback:logback-classic:1.5.18")
            .and("org.slf4j:slf4j-api:2.0.17");
    
}
```

We can reuse the code in `jeka-src` of plugin-1 module, for instance, as below:

```java title="plugins/plugin-1/jeka-src/Plugin1Custom.java"
import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.tool.JkDep;
import dev.jeka.core.tool.KBean;

@JkDep("../plugin-common")
class Plugin1Custom extends KBean {

    private JkDependencySet logLibs = PluginCommon.LOG_LIBS;
    
}
```
The specified path is relative to the module declaring the annotation.

### Import KBeans from other modules

You may use some KBeans from other modules. For example you want to run kbean action for a given list of child modules.

```java title="plugins/plugin-1/jeka-src/Custom.java"
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.project.JkProjectPackaging;
import dev.jeka.core.tool.JkInject;
import dev.jeka.core.tool.JkPostInit;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;

import java.nio.file.Path;

class Custom extends KBean {

    @JkInject("../../core")
    private ProjectKBean coreProjectKBean;

    @JkInject("../plugin-common")
    private ProjectKBean pluginCommonProjectKBean;

    public void doXxxx() {
        coreProjectKBean.pack();
        pluginCommonProjectKBean.pack();
        Path coreClasses = coreProjectKBean.project.compilation.layout.resolveClassDir();
    }

    @JkPostInit
    private void postInit(ProjectKBean projectKBean) {
        JkProject project = projectKBean.project;
        project.flatFacade
                .setMainArtifactJarType(JkProjectPackaging.JarType.FAT)
                .dependencies.compile
                .add(coreProjectKBean.project.toDependency());
    }
    
}
```

### Import runbases from other modules

In similar way, we can import runbases from other modules.

```java title="jeka-src/Custom.java"
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.tool.JkInject;
import dev.jeka.core.tool.JkRunbase;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;

class Custom extends KBean {

    @JkInject("core")
    private JkRunbase coreRunbase;

    public void delegate() {
        JkProject coreProject = coreRunbase.load(ProjectKBean.class).project;
        coreProject.test.run();
        coreProject.pack.run();
    }
    
}
```

### Discover child modules

From a parent module we can programmatically access the child modules.

```java title="jeka-src/MasterBuild.java"
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.testing.JkTestSelection;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.tooling.maven.MavenKBean;

import java.util.Optional;

class MasterBuild extends KBean {

    public boolean runIT = true;

    public void doAll() {
        this.getRunbase().getChildRunbases().stream()
                .map(runbase -> runbase.find(ProjectKBean.class))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(this::make);
        this.getRunbase().getChildRunbases().stream()
                .map(runbase -> runbase.load(MavenKBean.class))
                .forEach(MavenKBean::publish);
    }

    private void make(ProjectKBean projectKBean) {
        JkProject project = projectKBean.project;
        project.test.selection.addExcludePatternsIf(!runIT, JkTestSelection.IT_PATTERN);
        project.test.run();
        project.pack.run();
    }
}
```



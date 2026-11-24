# Base KBean

<!-- header-autogen-doc -->

[`BaseKBean`](https://github.com/jeka-dev/jeka/blob/master/core/src/main/java/dev/jeka/core/tool/builtins/base/BaseKBean.java) is similar to `ProjectKBean`, but it facilitates building JVM-based code hosted entirely in the *jeka-src* folder with a simpler classpath organization.

- **Single Classpath**: By default, there is a single classpath. However, if a `_dev` package exists in the code structure, its contents are excluded when creating JARs, native executables, or Docker images. Typically, build and test classes are placed in `_dev` for application builds.
- **Dependency Declaration**: Dependencies are declared by annotating any class with the `@JkDep` annotation. Dependencies within the `_dev` package are excluded from production artifacts.

**Key Features**

- Resolves dependencies, compiles code, and runs tests.
- Creates various types of JAR files out-of-the-box: regular, fat, shaded, source, and Javadoc JARs.
- Infers project versions from Git metadata.
- Executes packaged JARs.
- Displays dependency trees and project setups.
- Scaffolds skeletons for new projects.
- **Plugin Development**: Particularly well-suited for developing reusable JeKa plugins (KBeans), as it allows you to write your plugin code directly in `jeka-src` and test it immediately.

## Usage

Create an application, a JeKa plugin, or a simple script:
```shell
jeka base: scaffold scaffold.kind=APP
jeka base: scaffold scaffold.kind=PLUGIN
jeka base: scaffold
```

Build and run:
```shell
jeka base: build              # run tests, then create the JAR
jeka base: pack               # create the JAR only
jeka base: runMain            # run the application from its classes
jeka base: runJar programArgs="arg1 arg2"
jeka base: info               # show the main class, classpath, manifest, ...
```

## Configuration

```properties title="jeka.properties"
# Make 'base:' optional on the command line (e.g. 'jeka pack')
jeka.kbean.default=base

# Needed for publishing
@base.moduleId=org.example:my-app
@base.gitVersioning.enable=true

# FAT by default
@base.jarType=REGULAR

# Used by runMain and runJar
@base.jvmOptions=-Xmx256m
@base.programArgs=arg1 arg2
```

There is no `version` property. Use `@base.gitVersioning.enable=true` as above, or `@maven.pub.version` for publishing. 
See [Maven KBean](kbeans-maven.md).

### Dependencies

Declare dependencies by annotating classes. Those declared in the `_dev` package are for tests and build only:

```java title="jeka-src/_dev/Custom.java"
package _dev;

import dev.jeka.core.tool.JkDep;
import dev.jeka.core.tool.JkPostInit;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.base.BaseKBean;

@JkDep("org.junit.jupiter:junit-jupiter")
@JkDep("org.junit:junit-bom:6.0.3@pom")
class Custom extends KBean {

    @JkPostInit(required = true)
    private void postInit(BaseKBean baseKBean) {
        baseKBean.setModuleId("org.example:my-app").setVersion("1.0.0");
        baseKBean.packActions.append("say-done", () -> System.out.println("Packed!"));
    }
}
```

The `postInit` method customizes the build. Add `@custom=on` to *jeka.properties* to have `Custom` always initialized.

!!! note
    Declare one `@JkDep` per line, as annotations are read from the source code before compilation.

**Example**

- [Base Application](https://github.com/jeka-dev/demo-base-application): The `BaseKBean` is set as the default KBean in `jeka.properties`. The accompanying `README.md` file details the available `base:` methods that can be invoked.

## Summary

<!-- body-autogen-doc -->
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

**Example**

- [Base Application](https://github.com/jeka-dev/demo-base-application): The `BaseKBean` is set as the default KBean in `jeka.properties`. The accompanying `README.md` file details the available `base:` methods that can be invoked.

<!-- body-autogen-doc -->
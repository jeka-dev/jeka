# Native API

The **Native API** offers a [`JkNativeCompilation` class](https://github.com/jeka-dev/jeka/blob/master/core/src/main/java/dev/jeka/core/api/tooling/nativ/JkNativeCompilation.java) that creates an executable file from compiled classes or JARs. 

An easy way to get an instance is to call `NativeKBean#createNativeCompilation`, passing the project as a `JkBuildable`. 
This prepares everything for native compilation, applying the settings of the [Native KBean](kbeans-native.md). 
You can also start from a classpath, using `JkNativeCompilation.ofClasspath`. The class automatically downloads GraalVM, 
which includes the `native-image` executable used for compiling to native - you don't need to care about GraalVM installation. 

It also provides helpful methods to add resources to the native executable.

```java title="Example"
nativeKBean.createNativeCompilation(project.asBuildable())
    .setIncludesAllResources(true)
    .make(Paths.get("jeka-output/my-app"));
```
This code performs the following actions:

- Checks if JeKa is running on *GraalVM*. If not, it fetches *GraalVM* 25 (if not already available).
- Includes all project resources located in *src/main/resources*.
- Fetches the metadata repository and adds it to the compilation classpath.
- Uses the `native-image` *GraalVM* tool to compile the application into a native binary.

By default, *JeKa* will fetch *GraalVM* using the Foojay API unless the `JEKA_GRAALVM_HOME` environment variable is set.  
If the environment variable's value starts with `DOWNLOAD_` (e.g., `DOWNLOAD_21`), the specified GraalVM version will be downloaded from the internet.
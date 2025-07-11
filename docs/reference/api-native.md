# Native API

The **Native API** offers a [`JkNativeCompilation` class](https://github.com/jeka-dev/jeka/blob/master/core/src/main/java/dev/jeka/core/api/tooling/nativ/JkNativeCompilation.java) that creates an executable file from compiled classes or JARs. 

An easy way to use it is by passing a `JkProject` when instantiating.
This setup prepares everything for native compilation. The class automatically downloads GraalVM, 
which includes the `nativeImage` executable used for compiling to native - you don't need to care about GraalVM installation. 

It also provides helpful methods to add resources to the native executable.

```java title="Exemple"
JkProject project = ...
        
JkNativeCompilation.of(project.asBuildable()
    .setIncludesAllResources(true)
    .make(Paths.get("build/my-app.exe"));
```
This code performs the following actions:
- Checks if the system is running on *GraalVM*. If not, it fetches the latest *GraalVM* version (if not already available).
- Includes all project resources located in *src/main/resources*.
- Fetches the metadata repository and adds it to the compilation classpath.
- Uses the `native-image` *GraalVM* tool to compile the application into a native binary.

By default, *JeKa* will fetch *GraalVM* using the Foodjay API unless the `JEKA_GRAALVM_HOME` environment variable is set.  
If the environment variable's value starts with `DOWNLOAD_` (e.g., `DOWNLOAD_21`), the specified GraalVM version will be downloaded from the internet.
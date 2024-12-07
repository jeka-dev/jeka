# Native API
The **Native API** offers a [`JkNativeCompilation` class](https://github.com/jeka-dev/jeka/blob/master/dev.jeka.core/src/main/java/dev/jeka/core/api/tooling/nativ/JkNativeCompilation.java) that creates an executable file from compiled classes or JARs. 

An easy way to use it is by passing a `JkProject` when instantiating.
This setup prepares everything for native compilation. The class automatically downloads GraalVM, 
which includes the `nativeImage` executable used for compiling to native - you don't need to care about GraalVM installation. 

It also provides helpful methods to add resources to the native executable.

```java title="Exemple"
JkProject project = JkProject.of();
project.flatFacade.dependencies.compile
    .add("com.google.guava:guava:21.0")
    .add("com.github.djeang:vincer-dom:1.2.0");

JkNativeCompilation.of(project.asBuildable()
    .setIncludesAllResources(true)
    .make(Paths.get("build/my-app.exe"));
```

# Source-Runnable Applications

With JeKa, you don't need to create and publish binaries to publish your application. JeKa acts as an application source manager, enabling you to run applications directly from their Git (remote) repository or to install them on a user's desktop by building them at installation time.

## Running Applications from Remote Repositories

### Basic Usage

To run an application directly from its Git repository, use the following command:

```bash
jeka --remote GIT_URL --program PROGRAM_ARGUMENTS
```
or
```bash
jeka -r GIT_URL -p PROGRAM_ARGUMENTS
```

**Example:**
```bash
jeka -r https://github.com/djeang/demo-dir-checksum -p -a SHA256
```
You can pass `-Dxxxxx=yyyy` as a program argument to set system properties.

### Browsing the Catalog

You can see a list of available applications by executing:
```bash
jeka app: catalog
```

### Running Specific Versions

You can run a specific version of an application by appending a tag name to the URL:

```bash
jeka -r https://github.com/jeka-dev/demo-cowsay#0.0.6 -p "Hello JeKa!"
```

### Trust and Security

When you run a remote application for the first time, JeKa will prompt you to confirm whether you trust the source URL. This prevents the accidental execution of malicious code.

If you accept, JeKa clones the repository, builds the application, and runs it. On subsequent runs, it executes directly using the cached binaries.

You can manage trusted URLs in your `[JEKA_HOME]/global.properties` file:
```properties
jeka.app.url.trusted=github.com/djeang/
```
In this example, any repository starting with `github.com/djeang/` will be trusted automatically.

## Performance and Shortcuts

### Native Executables

To improve startup performance, you can compile a remote application to a native executable. Once compiled, all subsequent runs will use the native version.

```bash
jeka -r https://github.com/jeka-dev/demo-cowsay#0.0.9 native: compile
```

### Command Shorthands

Typing and remembering full repository URLs can be tedious. You can simplify this by using shortcuts in your `~/.jeka/global.properties` file:

```properties
jeka.cmd.cowsay=-r https://github.com/jeka-dev/demo-cowsay#0.0.9 -p
```
Now you can invoke the application using the `::` prefix:
```bash
jeka ::cowsay "Hello World!"
```

## Desktop Installation

JeKa lets you install applications for direct execution from your terminal, without needing to call `jeka` explicitly.

```bash
jeka app: install repo=https://github.com/djeang/kill8
```
Or use a shorthand for known repositories:
```bash
jeka app: install repo=kill8@djeang
```

For faster startup, you can install the native version:
```bash
jeka app: install repo=kill8@djeang runtime=NATIVE
```

Once installed, you can simply call the application by name:
```bash
kill8 8081
```

For more details on application management, see the [App KBean Reference](../reference/kbeans-app.md).

## Creating Source-Runnable Applications

### JeKa Projects

Applications built with JeKa are "source-runnable" by default. 

When running a remote application, JeKa clones the repository and builds it using `jeka base: pack` (or `jeka project: pack` if a project is detected). It then looks in the `jeka-output` directory to run the resulting JAR or native binary.


### Maven Projects

Maven projects can be installed from source using the `maven:` KBean. 
Add a `jeka.properties` file to your Maven project root to configure the build delegation:

```properties
jeka.java.version=21

# Delegate build to Maven wrapper
jeka.program.build=maven: wrapPackage

# For native builds (requires GraalVM)
jeka.java.distrib=graalvm
jeka.program.build.native=maven: wrapPackage args="-Pnative"
```

For multi-module projects, specify which module contains the application:
```properties
jeka.program.build=maven: wrapPackage appModule=app
```

**Requirements:**
1. **Maven wrapper**: Include `mvnw` / `mvnw.cmd` in your repository (to make the build fully portable)
2. **Fat JAR plugin**: Configure `maven-shade-plugin` to create executable JARs.
3. **Native support (optional)**: Add `native-maven-plugin` in a Maven profile.

See a complete example at [demo-maven-multimodule](https://github.com/jeka-dev/demo-maven-multimodule).

### Custom Build Commands

For other build tools or custom requirements, you can create a custom KBean to handle the build process:

```properties
jeka.program.build=custom: build
jeka.program.build.native=custom: buildNative
```

```java
class Custom extends KBean {

    @JkDoc("Build application and copy result to jeka-output")
    public void build() {
        gradle("clean assemble");
        copyToJekaOutput();
    }

    @JkDoc("Build native application")
    public void buildNative() {
        gradle("clean nativeCompile");
        copyToJekaOutput();
    }

    private void gradle(String gradleArguments) {
        JkLog.info("Executing gradlew " + gradleArguments);
        String distrib = getRunbase().getProperties().get("jeka.java.distrib", "graalvm");
        String javaVersion = getRunbase().getProperties().get("jeka.java.version", "21");
        String distribFolder = distrib + "-" + javaVersion;
        Path graalvmHome = JkLocator.getCacheDir().resolve("jdks").resolve(distribFolder);
        String newPath = graalvmHome.resolve("bin") + File.pathSeparator + System.getenv("PATH");
        JkProcess.ofWinOrUx("gradlew.bat", "./gradlew")
                .addParamsAsCmdLine(gradleArguments)
                .setWorkingDir(getBaseDir())
                .setEnv("JAVA_HOME", graalvmHome.toString())
                .setEnv("GRAALVM_HOME", graalvmHome.toString())
                .setEnv("PATH", newPath)
                .setInheritIO(true)
                .exec();
    }

    private void copyToJekaOutput() {
        JkPathTree.of(getBaseDir().resolve("build/libs"))
                .andMatching("*.jar")
                .copyTo(getBaseDir().resolve(JkConstants.OUTPUT_PATH),
                        StandardCopyOption.REPLACE_EXISTING);
        Path nativeDir = getBaseDir().resolve("build/native/nativeCompile");
        if (Files.exists(nativeDir)) {
            JkPathTree.of(nativeDir).copyTo(getBaseDir().resolve(JkConstants.OUTPUT_PATH),
                        StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
```

## Docker Integration

JeKa can create Docker images directly from remote application sources.

**Create a JVM image:**
```bash
jeka -r https://github.com/jeka-dev/demo-cowsay docker: build
```

**Create a native image:**
```bash
jeka -r https://github.com/jeka-dev/demo-cowsay docker: buildNative
```

## See Also

- [App KBean Reference](../reference/kbeans-app.md) - Detailed documentation on application installation and management.
- [Native KBean Reference](../reference/kbeans-native.md) - Learn more about native compilation features.
- [Docker KBean Reference](../reference/kbeans-docker.md) - Learn more about Docker integration.







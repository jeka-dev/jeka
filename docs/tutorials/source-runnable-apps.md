# Source-Runnable Applications

With JeKa, distributing software doesn't require creating and publishing binaries. JeKa acts as a source-based application manager, letting you run programs directly from their remote Git repository or install them on a user's machine by building them at installation time.

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

You can see a list of available application catalogs by executing:
```bash
jeka app: catalog
```

To see applications within a specific catalog:
```bash
jeka app: catalog name=demo
```
This will list the application names, descriptions, and commands to run or install them.

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

JeKa lets you install applications for direct execution from your terminal, making them available in your system `PATH`.

```bash
jeka app: install repo=https://github.com/djeang/kill8
```
Or use a shorthand for known repositories:
```bash
jeka app: install repo=kill8@djeang
```

For faster startup, you can install the native version (requires GraalVM):
```bash
jeka app: install repo=kill8@djeang runtime=NATIVE
```

Once installed, you can simply call the application by name:
```bash
kill8 8081
```

### Managing Installed Applications

To list all applications installed on your system via JeKa:
```bash
jeka app: list
```

To update an application to its latest version (highest semantic tag or latest commit):
```bash
jeka app: update name=kill8
```

To remove an application from your system:
```bash
jeka app: uninstall name=kill8
```

### Desktop GUI Applications

If the application is a GUI, you can install it as a standalone desktop application.
```bash
jeka app: install repo=kill8@djeang runtime=BUNDLE
```
This will create a .dmg (macOS) or .exe (Windows) installer that you can use to install the application on your desktop.

For more details on application management, see the [App KBean Reference](../reference/kbeans-app.md).

## Creating Source-Runnable Applications

### JeKa Projects

Applications built with JeKa are "source-runnable" by default. 

When running or installing a remote application, JeKa clones the repository and builds it:

1. It tries to execute the command specified in the `jeka.program.build` property from the project's `jeka.properties`.
2. If this property is missing, it runs `jeka project: pack pack.jarType=FAT pack.detectMainClass=true` (for Java projects with a `src` directory).
3. Otherwise, it runs `jeka base: pack`.

For native execution (or when `runtime=NATIVE`), JeKa:
1. Tries to execute the command specified in `jeka.program.build.native`.
2. If missing, it uses `jeka.program.build` followed by `native: compile`.
3. If both are missing, it runs `native: compile`.

JeKa then looks in the `jeka-output` directory to run the resulting JAR or native binary.


### Maven Projects

Maven projects can be installed from source using the `maven:` KBean. 
Add a `jeka.properties` file to your Maven project root to configure the build delegation:

```properties
jeka.java.version=21

# Delegate build to Maven wrapper
jeka.program.build=maven: wrapPackage

# Optional: use this for native builds. Set the GraalVM version used to build the native executable.
jeka.program.build.native=maven: wrapPackage args="-Pnative" -Djeka.java.distrib=graalvm -Djeka.java.version=25
```

For multi-module projects, specify which module contains the application:
```properties
jeka.program.build=maven: wrapPackage appModule=app
```

For desktop applications (installed using `runtime=BUNDLE`), specify the *Maven* command and the location of the distribution:
```properties
jeka.program.build.bundle=maven: wrapPackage args="-Pjpackage"
jeka.program.bundle.dist=target/dist
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

## Examples

- [CLI app with Native mode (Jeka)](https://github.com/djeang/kill8)
- [Desktop app bundled as standalone (Jeka)](https://github.com/djeang/Calculator-jeka)
- [CLI app with Maven in JVM or Native mode](https://github.com/jeka-dev/demo-cowsay-maven)
- [JavaFX Desktop app bundled as standalone (Maven)](https://github.com/djeang/devtools-maven)
- [Same JavaFX app but using Jeka](https://github.com/djeang/devtools) - *Simpler configuration than Maven*

## See Also

- [App KBean Reference](../reference/kbeans-app.md) - Detailed documentation on application installation and management.
- [Native KBean Reference](../reference/kbeans-native.md) - Learn more about native compilation features.
- [Docker KBean Reference](../reference/kbeans-docker.md) - Learn more about Docker integration.







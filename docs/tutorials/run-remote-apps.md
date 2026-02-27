# Run Remote Applications

JeKa allows running or installing applications directly from their source code hosted on a Git repository.

## Run

You can have a look at the application catalog by executing:
```bash
jeka app: catalog
```

To run an application directly, execute:
```bash
jeka --remote GIT_URL --program PROGRAM_ARGUMENTS
```
or
```bash
jeka -r GIT_URL -p PROGRAM_ARGUMENTS
```
Example:
```bash
jeka --remote https://github.com/djeang/demo-dir-checksum --program -a SHA256
```
You can pass `-Dxxxxx=yyyy` as a program argument to set system properties.

When you run it for the first time, JeKa will prompt you to confirm whether you trust the URL `github.com/djeang/demo-dir-checksum`.
This is to prevent the execution of malicious code.

If you accept, the application will build before running. On subsequent runs, it will execute directly, as the binaries are cached.

Later you can edit the `[JEKA_HOME]/global.properties` file to adjust the trusted URLs.

Example:
```properties
jeka.app.url.trusted=github.com/djeang/
```
In this example, we shorten the URL, so that any URL starting with `github.com/djeang/` will be trusted.

## Run a Specific Version
You can run a specific version of an application by specifying a tag name in the URL. For example:

```bash
jeka -r https://github.com/jeka-dev/demo-cowsay#0.0.6 -p "Hello JeKa!"
```

## Run Native Executables
You can compile a remote application to a native executable. Once compiled, all subsequent runs will execute the native version of the application.

To compile a remote application into a native executable, use the following command:

```bash
jeka -r https://github.com/jeka-dev/demo-cowsay#0.0.6 native: compile
```

After compilation, run the application as usual.

## Build Docker Image of the application
JeKa allows creating Docker images of a remote application.

Create a JVM image:
```bash
jeka -r https://github.com/jeka-dev/demo-cowsay docker: build
```

Create a native image:
```bash
jeka -r https://github.com/jeka-dev/demo-cowsay docker: buildNative
```
Then follow the instructions to run the built Docker image.

## Use Shorthands
Typing and remembering the repository URL of the application for every run can be tedious. You can simplify this by using JeKa's global command shortcut substitution mechanism.

For example, define the following property in your `~/.jeka/global.properties` file:

```properties
jeka.cmd.cowsay=-r https://github.com/jeka-dev/demo-cowsay#0.0.6 -p
```
Now, you can invoke the application using the shortcut command:
```bash
jeka ::cowsay "Hello World!"
```

## Install

JeKa lets you install apps for direct execution (no need to use `jeka`). Example:

```bash
jeka app: install repo=https://github.com/djeang/kill8
```
Or
```bash
jeka app: install repo=kill8@djeang
```

For a faster cold start, install the native version:
```bash
jeka app: install repo=kill8@djeang native:
```

These commands install the `kill8` application in the user's PATH, allowing you to simply invoke the application by name to run it:
```bash
kill8 8081
```

For more details, refer to the [documentation](../reference/kbeans-app.md).

## Write Applications for Direct Run/Installation

Applications built with *JeKa* are automatically "source runnable" by default.

When running a remote application for the first time, *JeKa* clones the repository then builds it with `jeka base: pack`
or `jeka project: pack` if a project is detected.

It then looks in the `jeka-output` directory to run the native or JAR file.

### Install Maven Projects

Projects built with Maven can be installed from source. JeKa provides a `maven:` KBean for this.

#### Simple Maven Project

Add a `jeka.properties` file to your Maven project root:

```properties
jeka.java.version=21

# Delegate build to Maven wrapper
jeka.program.build=maven: wrapPackage

# For native builds (requires GraalVM)
jeka.java.distrib=graalvm
jeka.program.build.native=maven: wrapPackage args="-Pnative"
```

Users can now install your app:

```bash
jeka app: install repo=https://github.com/your-org/your-app
```

Or install as native:

```bash
jeka app: install repo=https://github.com/your-org/your-app runtime=NATIVE
```

#### Maven Multi-Module Projects

For multi-module projects, specify which module contains the application:

```properties
jeka.java.version=21
jeka.java.distrib=graalvm

jeka.program.build=maven: wrapPackage appModule=app

jeka.program.build.native=maven: wrapPackage appModule=app args="-Pnative"
```

**Requirements:**

1. **Maven wrapper**: Include `mvnw` / `mvnw.cmd` for portable builds
2. **Fat JAR plugin**: Configure `maven-shade-plugin` to create executable JARs:
   ```xml
   <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-shade-plugin</artifactId>
       <version>3.5.1</version>
       <executions>
           <execution>
               <phase>package</phase>
               <goals>
                   <goal>shade</goal>
               </goals>
               <configuration>
                   <transformers>
                       <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                           <mainClass>com.example.Main</mainClass>
                       </transformer>
                   </transformers>
               </configuration>
           </execution>
       </executions>
   </plugin>
   ```
3. **Native support (optional)**: Add `native-maven-plugin` in a profile:
   ```xml
   <profile>
       <id>native</id>
       <build>
           <plugins>
               <plugin>
                   <groupId>org.graalvm.buildtools</groupId>
                   <artifactId>native-maven-plugin</artifactId>
                   <version>0.10.3</version>
                   <executions>
                       <execution>
                           <goals>
                               <goal>compile-no-fork</goal>
                           </goals>
                           <phase>package</phase>
                       </execution>
                   </executions>
               </plugin>
           </plugins>
       </build>
   </profile>
   ```

See a complete example at [demo-maven-multimodule](https://github.com/jeka-dev/demo-maven-multimodule).

### Custom Build Commands

For other build tools or custom needs, create a custom KBean with a build method:

```properties
jeka.program.build=custom: build
jeka.program.build.native=custom: buildNative
```

```java
class Custom extends KBean {

    @JkDoc("Build application and copy result to jeka-output")
    public void build() {
        mvn("clean package -DskipTests");
        copyToJekaOutput();
    }

    @JkDoc("Build native application")
    public void buildNative() {
        mvn("clean package -DskipTests -Pnative");
        copyToJekaOutput();
    }

    private void mvn(String mvnArguments) {
        JkLog.info("Executing mvn " + mvnArguments);
        String distrib = getRunbase().getProperties().get("jeka.java.distrib", "graalvm");
        String javaVersion = getRunbase().getProperties().get("jeka.java.version", "21");
        String distribFolder = distrib + "-" + javaVersion;
        Path graalvmHome = JkLocator.getCacheDir().resolve("jdks").resolve(distribFolder);
        String newPath = graalvmHome.resolve("bin") + File.pathSeparator + System.getenv("PATH");
        JkProcess.ofWinOrUx("mvnw.cmd", "./mvnw")
                .addParamsAsCmdLine(mvnArguments)
                .setWorkingDir(getBaseDir())
                .setEnv("JAVA_HOME", graalvmHome.toString())
                .setEnv("GRAALVM_HOME", graalvmHome.toString())
                .setEnv("PATH", newPath)
                .setInheritIO(true)
                .exec();
    }

    private void copyToJekaOutput() {
        JkPathTree.of(getBaseDir().resolve("target"))
                .andMatching("*.jar", "*-runner")
                .copyTo(getBaseDir().resolve(JkConstants.OUTPUT_PATH),
                        StandardCopyOption.REPLACE_EXISTING);
    }
}
```

Keep builds portable by using tool wrappers (Maven wrapper, Gradle wrapper, etc.).







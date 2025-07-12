# Run Remote Applications

JeKa allows running or installing applications directly from their source code hosted on Git repository.

## Run

You can have a look at the application catalog by executing:
```shell
jeka app: catalog
```

To run directly an application, execute:
```shell
jeka --remote <git-url> --program <program arguments>
```
or
```shell
jeka -r <git-url> -p <program arguments>
```
Example:
```shell
jeka --remote https://github.com/djeang/demo-dir-checksum --program -a SHA256
```
you can pass `-Dxxxxx=yyyy` as program argument to set system properties.

When you run it for the first time, Jeka will prompt you to confirm whether you trust the URL `github.com/djeang/demo-dir-checksum`.
This is to prevent the execution of malicious code.

If you accept, the application will build before running. On subsequent runs, it will execute directly, as the binaries are cached.

Later you can edit the `[JEKA_HOME]/global.properties` file to adjust the trusted URLs.

Example:
```properties
jeka.app.url.trusted=github.com/djeang/
```
In this example, we shorten the url, so that any url starting by `github.com/djeang/`will be trusted.

## Run a Specific Version
You can run a specific version of an application by specifying a tag name in the URL. For example:

```shell
jeka -r https://github.com/jeka-dev/demo-cowsay#0.0.6 -p "Hello JeKa!"
```

## Run Native Executables
You can compile a remote application to a native executable. Once compiled, all subsequent runs will execute the native version of the application.

To compile a remote application into a native executable, use the following command:

```jeka
jeka -r https://github.com/jeka-dev/demo-cowsay#0.0.6 native: compile
```

After compilation, run the application as usual.

## Build Docker Image of tha application
Jeka allow creating Docker images of a remote application.

Create a JVM image:
```shell
jeka -r https://github.com/jeka-dev/demo-cowsay docker: build
```

Create a native image:
```shell
jeka -r https://github.com/jeka-dev/demo-cowsay docker: buildNative
```
.
Then follow the instruction to run the built docker image.

## Use Shorthands
Typing and remembering the repository URL of the application for every run can be tedious. You can simplify this by using Jeka's global command shortcut substitution mechanism.

For example, define the following property in your `~/.jeka/global.properties` file:

```properties
jeka.cmd.cowsay=-r https://github.com/jeka-dev/demo-cowsay#0.0.6 -p
```
Now, you can invoke the application using the shortcut command:
```shell
jeka ::cowsay "Hello World!"
```

## Install

JeKa lets you install apps for direct execution (no need to use `jeka`). Example:

```shell
jeka app: install repo=github.com/djeang/kill8
```
For faster cold start, install the native version:
```shell
jeka app: install repo=github.com/djeang/kill8 native=
```

These commands install the `kill8` application in the user's PATH, allowing you to simply invoke the application by name to run it:
```shell
kill8 8081
```

For more details, refer to the [documentation](/reference/kbeans-app).

## Write Applications for Direct Run/Installation.

Applications built with *Jeka* are normally automatically made *source runnable" by default.

When Running a remote application for the first time, *Jeka* clones the directory then build it with `jeka base: pack` 
or `jeka project: pack` if a project is detected.

Then it looks in the jeka *jeka-output* dir to run the native or jar file.

### Set a Custom Build Command

If your project needs a specific build command or uses a tool like *Maven*, you can set the Jeka command for building.

The command must be a *Jeka* KBean command, not a shell command. 
For example, to build with *Maven*, you need to write a `Custom` Kbean defining the build method, and mention it in *jeka.properties* file.

```properties
jeka.program.build=custom: build

## you can also specify the command for native builds
jeka.program.build.native=custom: build
```


```java
class Custom extends KBean {

    @JkDoc("Build application and copy result in jeka-output in order to be run with '-p' option")
    public void build() {
        mvn("clean package -DskipTests -Pnative");
        copyToJekaOutput();
    }
    
    private void mvn(String mvnArguments) {
        JkLog.info("Executing mvn " + mvnArguments);
        String distrib = getRunbase().getProperties().get("jeka.java.distrib", "graalvm");
        String javaVersion = getRunbase().getProperties().get("jeka.java.version", "22");
        String distribFolder = distrib + "-" + javaVersion;
        Path graalvmHome = JkLocator.getCacheDir().resolve("jdks").resolve(distribFolder);
        String newPath =  graalvmHome.resolve("bin") + File.pathSeparator + System.getenv("PATH");
        JkProcess.ofWinOrUx("mvnw.cmd", "./mvnw")
                .addParamsAsCmdLine(mvnArguments)
                .addParamsIf(System.getProperties().containsKey("jeka.test.skip"), "-Dmaven.test.skip=true")
                .setWorkingDir(getBaseDir())
                .setEnv("JAVA_HOME", graalvmHome.toString())
                .setEnv("GRAALVM_HOME", graalvmHome.toString())
                .setEnv("PATH", newPath)
                .setInheritIO(true)
                .exec();
    }

    private void copyToJekaOutput() {
        JkPathTree.of(getBaseDir().resolve("target")).andMatching("*.jar", "*-runner")
                .copyTo(getBaseDir().resolve(JkConstants.OUTPUT_PATH), StandardCopyOption.REPLACE_EXISTING);
    }

}
```

The main point is to keep the build portable, like using the *Maven* wrapper.







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

Jeka allows you to install an application, enabling you to execute it without invoking `jeka`.

Example:
```shell
jeka app: install repo=github.com/djeang/kill8
```
This command installs the `kill8` application in the user's PATH, allowing you to simply invoke the application by name to run it:
```shell
kill8 8081
```

For more details, refer to the [documentation](/reference/kbeans-app).




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

Find more details [here](/reference/kbeans-app)




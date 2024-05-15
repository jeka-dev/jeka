#

## JeKa commands

Create jar
```shell
jeka project: pack
```

Clean and Create jar
```shell
jeka project: pack --clean
```

Create jar skipping tests
```shell
jeka project: pack "-Djeka.skip.tests=true"
```

Run jar
```shell
jeka project: runJar run.programArgs="" run.jvmOptions=""
```

Synchronize IntelliJ
```shell
jeka intellij: iml
```

Create docker image
```shell
jeka docker: build

```
Run docker image
```shell
jeka docker: run
```
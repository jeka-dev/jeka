#

## JeKa commands

Clean and Create jar
```shell
jeka project: pack
```

Create jar skipping tests
```shell
jeka project: pack -Djeka.skip.tests=true
```

Run jar
```shell
jeka project: runJar run.programArgs="" run.jvmOptions=""
```

Create docker image
```shell
jeka docker: build
```
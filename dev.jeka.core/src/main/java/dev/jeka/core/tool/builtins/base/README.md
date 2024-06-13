#

## JeKa commands

Execute tests
```shell
jeka base: test
```

Create jar 
```shell
jeka base: pack
```

Run jar
```shell
jeka base: runJar programArgs="" jvmOptions=""
```
Show info about docker image
```shell
jeka docker: info
```

### Docker

Create image
```shell
jeka docker: build
```
Show info about image
```shell
jeka docker: info
```
Run image
```shell
jeka docker: run
```
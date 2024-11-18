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

Run jar 
This will directly run the built application, without checking if sources have changed since last run
```
jeka -p
```

### Docker

Create image:
```shell
jeka docker: build
```
Show info about image
```shell
jeka docker: info
```

Create native image
```shell
jeka docker: buildNative
```
Show info about native image
```shell
jeka docker: infoNative
```


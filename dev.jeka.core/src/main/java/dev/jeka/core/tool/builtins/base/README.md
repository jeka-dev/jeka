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

Show info about docker image
```shell
jeka docker: info
```

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
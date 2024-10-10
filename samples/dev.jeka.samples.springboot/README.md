# Sample for Jeka + Springboot

Warning:
This springboot project is highly used in integration test. Use it with caution.


## Play with native

```shell
jeka native: make "-Djeka.java.version=23" "-Djeka.java.distrib=graalvm"
```

## Maven setup

This project is also setup to be build with Maven, in order to investigate how maven works to 
achieve build tasks as native compilation.

```shell
mvn clean package -Pnative
```

```shell
mvn -Pnative native:compile
```

Make a native docker image
```shell
mvn -Pnative spring-boot:build-image
```
![Build Status](https://github.com/jerkar/protobuf-plugin/actions/workflows/push-master.yml/badge.svg)
[![Maven Central](https://img.shields.io/maven-central/v/dev.jeka/protobuf-plugin.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22dev.jeka22%20AND%20a:%22protobuf-plugin%22)

# Protobuf Plugin for Jeka

This plugin provides utility classes and KBean to compile Google ProtoBuffer .proto files.

## How to Use Declaratively

Declare :
```properties
jeka.cmd._append=@dev.jeka:protobuf-plugin protobuf#
```
This will add automatically protobuf code source generation to your project.

You can customize this behavior using properties :
```properties
# Change the location of proto files (default is src/main/proto)
protobuf#protoPath=src/main/protofiles

# Specify the version of the protoc compiler to use
protobuf#protocVersion=3.1.10

# Specify the version of protobuf library to include in compile-time dependencies
protobuf#protobufVersion=3.21.12
```

Execute `jeka protobuf#help` to get help on available options.

See available protoc compiler options [here](https://manpages.ubuntu.com/manpages/xenial/man1/protoc.1.html).

See how to use Protocol Buffers [here](https://protobuf.dev/)

## How to Use Programmatically

Just declare the plugin in your build class, as follows :

```java
@JkDefClasspath("dev.jeka:protobuf-plugin")
public class Build extends JkClass {

    ProtobufJkBean protobuf = getPlugin(ProtobufJkBean.class);
    ...
```


### Example

See example [here](../../samples/dev.jeka.samples.sonarqube)




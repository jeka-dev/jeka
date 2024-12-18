# Protobuf Plugin for JeKa

This plugin provides KBean for generating classes from ProtoBuffer .proto files.

It also contains classes for programmatic configuration and execution.

Resources:
  - Command-line documentation: `jeka protobuf: --doc`.
  - Source Code: [Visit here](src/dev/jeka/plugins/protobuf/ProtobufKBean.java).
  - Protoc compiler options: [Visit here](https://manpages.ubuntu.com/manpages/xenial/man1/protoc.1.html).
  - How to use Protocol Buffers: [Visit here](https://protobuf.dev/)

## Initialization

This KBean register a source generator to the *project KBean* if present.

## Configure using KBeans

There's no required configuration.

```properties
jeka.classpath.inject=dev.jeka:protobuf-plugin
@protobuf=

# Optional properties
@protobuf.protoPath=src/main/protofiles
@protobuf.protocVersion=3.1.10
@protobuf.protobufVersion=3.21.12
```

Properties:
  - protoPath: Change the location of proto files (default is src/main/proto)
  - protocVersion: Specify the version of the protoc compiler to use
  - protobufVersion: Specify the version of protobuf library to include in compile-time dependencies


## Programmatic Usage

Configure a project as follows :

```java
JkProject project = myProject();
JkProtobuf.of()
        .setProtobufVersion("3.25.1")
        .configure(project); // Adds the default protobuf source generator to the project.
```







# Protobuf Plugin for JeKa

Provides configuration and methods to compile protoBuffers files.

**This KBean post-initializes the following KBeans:**

| Post-initialised KBean | Description                     |
|------------------------|---------------------------------|
| ProjectKBean           | Adds Protobuf source generator. |


**This KBean exposes the following fields:**

| Field                       | Description                                                                                                                                                                                                                                         |
|-----------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| protoPath [String]          | The path (relative to project root) of directory containing the proto files.                                                                                                                                                                        |
| protocVersion [String]      | Version of protoc compiler. Should be a version of module 'com.github.os72:protoc-jar'.                                                                                                                                                             |
| extraProtocOptions [String] | Extra options to pass to protoc compiler. See https://manpages.ubuntu.com/manpages/xenial/man1/protoc.1.html.                                                                                                                                       |
| protobufVersion [String]    | The version of com.google.protobuf:protobuf-java to include in compile-time dependencies. If empty or null, this dependencies won't be included automatically. The version will be resolved against coordinate 'com.google.protobuf:protobuf-java'. |


Resources:
  - Source Code: [Visit here](src/dev/jeka/plugins/protobuf/ProtobufKBean.java).
  - Protoc compiler options: [Visit here](https://manpages.ubuntu.com/manpages/xenial/man1/protoc.1.html).
  - How to use Protocol Buffers: [Visit here](https://protobuf.dev/)
  - 
## Configure Example

```properties
jeka.classpath.inject=dev.jeka:protobuf-plugin
@protobuf=

# Optional properties
@protobuf.protoPath=src/main/protofiles
@protobuf.protocVersion=3.1.10
@protobuf.protobufVersion=3.21.12
```

## Programmatic Usage

Configure a project as follows :

```java
JkProject project = myProject();
JkProtobuf.of()
        .setProtobufVersion("3.25.1")
        .configure(project); // Adds the default protobuf source generator to the project.
```







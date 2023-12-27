# Protobuf Plugin for JeKa

This plugin provides utility classes and KBean to compile Google ProtoBuffer .proto files.

## Configure using KBeans

```properties
jeka.classpath.inject=dev.jeka:protobuf-plugin

# Instantiate protobuf KBean
jeka.cmd._append=protobuf#

# Change the location of proto files (default is src/main/proto)
protobuf#protoPath=src/main/protofiles

# Specify the version of the protoc compiler to use
protobuf#protocVersion=3.1.10

# Specify the version of protobuf library to include in compile-time dependencies
protobuf#protobufVersion=3.21.12
```
This will add automatically protobuf code source generation to your project.

See available protoc compiler options [here](https://manpages.ubuntu.com/manpages/xenial/man1/protoc.1.html).

See how to use Protocol Buffers [here](https://protobuf.dev/)

## Configure Programmatically

Just declare the plugin in your build class, as follows :

```java
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.plugins.protobuf.JkProtobuf;

@JkDefClasspath("dev.jeka:protobuf-plugin")
public class Build extends JkClass {

    @Override
    protected void init() {
        JkProject project = ...
        JkProtobuf.of()
                .setProtobufVersion("3.25.1")
                .configure(project); // Adds the default protobuf source generator to the project.
    }
}

```


### Example

See example [here](../../samples/dev.jeka.samples.sonarqube)




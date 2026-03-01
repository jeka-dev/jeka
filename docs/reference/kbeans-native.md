# Native KBean

<!-- header-autogen-doc -->

[`NativeKBean`](https://github.com/jeka-dev/jeka/blob/master/core/src/main/java/dev/jeka/core/tool/builtins/tooling/nativ/NativeKBean.java) enables native compilation for the [`project`](kbeans-project.md) and [`base` KBeans](kbeans-base.md).

**Key Features**

- Compiles classes into native executables.
- Automatically applies AOT metadata.
- Simplifies resource inclusion.
- Handles static linkage with minimal configuration.

## Usage

The `NativeKBean` works with either [`project`](kbeans-project.md) or [`base` KBeans](kbeans-base.md) to compile your Java application into a native executable using GraalVM native-image.

### Basic Invocation

```shell
jeka native: compile
```

This command:
1. Compiles your project if needed
2. Creates a native executable from the project's main artifact jar
3. Places the executable in the output directory (same location as the jar, but without `.jar` extension)

### Configuration

Configure the KBean through `jeka.properties` or programmatically in your KBean class.

**Example configuration in `jeka.properties`:**
```properties
@native.includeAllResources=true
@native.staticLink=MUSL
@native.metadataRepoVersion=0.10.3
@native.args=--verbose --no-fallback
```

**Available Configuration Options:**

- **`args`**: Extra arguments to pass to the native-image compiler (e.g., `--verbose --no-fallback`)
- **`staticLink`**: Static linkage mode for native libraries
    - `NONE` (default): No static linking
    - `MUSL`: Static linking with MUSL libc
    - `GLIBC`: Static linking with GLIBC
- **`useMetadataRepo`**: Uses predefined exploratory AOT metadata from the standard repository (default: `true`)
- **`metadataRepoVersion`**: Version of the GraalVM reachability metadata repository to use
- **`includeMainClassArg`**: Whether to specify the main class in command line arguments (default: `true`)
- **`includeAllResources`**: Includes all resources in the native image (default: `false`)

### Programmatic Configuration

You can also configure the `NativeKBean` programmatically:

```java
@JkDefClasspath("com.example:my-lib:1.0")
public class Build extends KBean {

    final NativeKBean nativeKBean = load(NativeKBean.class);

    Build() {
        nativeKBean.includeAllResources = true;
        nativeKBean.staticLink = JkNativeCompilation.StaticLink.MUSL;
        nativeKBean.args = "--verbose --no-fallback";
    }
}
```

### AOT Metadata

The KBean automatically handles AOT (Ahead-Of-Time) metadata required for reflection, resources, and JNI access in native images:

- By default, it uses the GraalVM reachability metadata repository
- The metadata repository version can be specified via `metadataRepoVersion`
- Disable with `@native.useMetadataRepo=false` if you provide your own metadata

### Requirements

- GraalVM is automatically downloaded if not already available
- The [`project`](kbeans-project.md) or [`base` KBean](kbeans-base.md) must be present in your build

See [native API](api-native.md) for low-level API details.

## Summary

<!-- body-autogen-doc -->
# Native KBean

<!-- autogen-doc -->

[`NativeKBean`](https://github.com/jeka-dev/jeka/blob/master/dev.jeka.core/src/main/java/dev/jeka/core/tool/builtins/tooling/nativ/NativeKBean.java) enables native compilation for *project* and *base* KBeans.

**Key Features**

- Compiles classes into native executables.
- Automatically applies AOT metadata.
- Simplifies resource inclusion.
- Handles static linkage with minimal configuration.

**Example of Configuration in jeka.properties:**
```properties
@native.includeAllResources=true
@native.staticLink=MUSL
@native.metadataRepoVersion=0.10.3
```

Invocation: `jeka native: compile`

See [native API](api-native.md).
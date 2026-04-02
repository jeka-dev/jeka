# IntelliJ KBean

<!-- header-autogen-doc -->

[`IntellijKBean`](https://github.com/jeka-dev/jeka/blob/master/core/src/main/java/dev/jeka/core/tool/builtins/tooling/ide/IntellijKBean.java) provides methods for generating IntelliJ IDEA metadata files.
It generates `.iml` module files and, when a `ProjectKBean` is present, also synchronizes `.idea/compiler.xml`.

## Sync

Run `jeka intellij: sync` (or `jeka intellij#sync`) to generate/update IntelliJ metadata.

What gets generated:
- The module `.iml` file with all classpath entries.
- `.idea/compiler.xml` — annotation-processor configuration and per-module javac options, derived from the project build definition.

## Annotation Processors in IntelliJ

When your project declares annotation-processor dependencies in the `[processor]` section of `jeka.project.deps`, the `sync` action automatically configures IntelliJ's built-in annotation processing by writing the resolved processor jars into `.idea/compiler.xml`. This means IntelliJ will run annotation processors (e.g. Lombok, MapStruct) natively during incremental builds, without needing a separate external build step.

## compiler.xml Sync Options

The `compilerOptions` group controls what is written into `.idea/compiler.xml`:

| Property | Description | Default |
| :--- | :--- | :--- |
| `compilerOptions.sync` | If `true`, updates `.idea/compiler.xml` on `sync`. | `true` |
| `compilerOptions.syncProcessorPaths` | If `true`, writes annotation-processor paths into `compiler.xml`. | `true` |
| `compilerOptions.syncJavacOptions` | If `true`, writes per-module javac options into `compiler.xml`. | `true` |

Example — disable compiler.xml sync entirely:
```properties
# jeka.properties
@intellij.compilerOptions.sync=false
```

## Customizing the .iml File

```java title="Configuration in a Build.java class"
@JkPostInit
private void postInit(IntellijKBean intellijKBean) {
    intellijKBean
            .replaceLibByModule("dev.jeka.jeka-core.jar", "core")
            .setModuleAttributes("core", JkIml.Scope.COMPILE, null);
}
```

## Summary

<!-- body-autogen-doc -->


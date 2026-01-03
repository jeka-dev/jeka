# IntelliJ KBean

<!-- header-autogen-doc -->

[`IntellijKBean`](https://github.com/jeka-dev/jeka/blob/master/core/src/main/java/dev/jeka/core/tool/builtins/tooling/ide/IntellijKBean.java) provides methods for generating metadata files for *IntelliJ* IDE. 
The content of an `.iml` file is computed according to the `JkBuildable` object found in the base directory.

This KBean provides methods to customize the generated `.iml` file.

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


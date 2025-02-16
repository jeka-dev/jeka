# Intellij KBean

<!-- autogen-doc -->

[`IntellijKBean`](https://github.com/jeka-dev/jeka/blob/master/dev.jeka.core/src/main/java/dev/jeka/core/tool/builtins/tooling/ide/IntellijKBean.java) provides methods for generating metadata files for _IntelliJ_ IDE. 
The content of an _iml_ file is computed according the `JkBuildable` object found in found in the base directory.

This _KBean_ proposes methods to customize generated *iml* file.

```java title="Configuration in a Build.java class"
@JkPostInit
private void postInit(IntellijKBean intellijKBean) {
    intellijKBean
            .replaceLibByModule("dev.jeka.jeka-core.jar", "dev.jeka.core")
            .setModuleAttributes("dev.jeka.core", JkIml.Scope.COMPILE, null);

```


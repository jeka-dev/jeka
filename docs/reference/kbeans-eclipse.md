# Eclipse KBean

<!-- header-autogen-doc -->

[`EclipseKBean`](https://github.com/jeka-dev/jeka/blob/master/core/src/main/java/dev/jeka/core/tool/builtins/tooling/ide/EclipseKBean.java) 
provides methods for generating metadata files for the *Eclipse* IDE, specifically the `.classpath` and `.project` files.

To ensure it works correctly, define a `JEKA_USER_HOME` classpath variable in Eclipse with the value `~/.jeka`.

## Usage

Generate or update the metadata files, then import or refresh the project in Eclipse:
```shell
jeka eclipse: sync
```
Run it again after changing dependencies or the source layout. An existing `.project` file is left unchanged.

In a directory containing several projects, generate the files of all sub-directories having a *jeka-src* directory:
```shell
jeka eclipse: all
```

## Configuration

```properties title="jeka.properties"
# Use absolute paths instead of the JEKA_USER_HOME variable
@eclipse.useVarPath=false

# Use a specific JRE
@eclipse.jreContainer=org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-21
```

## Summary

<!-- body-autogen-doc -->
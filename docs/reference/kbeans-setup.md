# Setup KBean

<!-- header-autogen-doc -->

[`SetupKBean`](https://github.com/jeka-dev/jeka/blob/master/core/src/main/java/dev/jeka/core/tool/builtins/setup/SetupKBean.java) 
is an [admin KBean](kbeans.md#location): it can be run from any directory, whether it contains a JeKa project or not.

## Edit Global Properties

Open the *global.properties* file in an editor. It contains [properties](properties.md) shared by all your projects, such as repositories or credentials.
```shell
jeka setup: editGlobalProps
```

Open the JeKa user home directory (`~/.jeka` by default), which contains this file:
```shell
jeka setup: openHomeDir
```

## Add a Command Shorthand

```shell
jeka setup: addShorthand content="build=project: pack sonarqube: run"
```
This adds `jeka.cmd.build=project: pack sonarqube: run` to *global.properties*. You can now run `jeka ::build` in any project.
See [Command shorthands](properties.md#command-shorthands).

## Update Shell Scripts

Add or update the `jeka` and `jeka.ps1` scripts in the current directory, so the project can be built without installing JeKa:
```shell
jeka setup: updateLocalScripts
```

## Summary

<!-- body-autogen-doc -->



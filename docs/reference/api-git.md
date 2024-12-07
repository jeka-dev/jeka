# Git API

The Git API provides classes for conveniently working with Git in Java.
The classes are part of the [`dev.jeka.core.api.tooling.git` package](https://github.com/jeka-dev/jeka/blob/master/dev.jeka.core/src/main/java/dev/jeka/core/api/tooling/git).

## [`JkGit` class](https://github.com/jeka-dev/jeka/blob/master/dev.jeka.core/src/main/java/dev/jeka/core/api/tooling/git/JkGit.java)

The `JkGit` class extends `JkProcess`, which means it inherits methods that make process execution easier.

```java title="Sequence og Git calls"
JkGit git = JkGit.of(rootDir);
git
    .execCmdLine("add .")
    .execCmdLine("config user.name  jeka-bot")
    .execCmdLine("config user.email jeka-bot@github-action.com")
    .execCmdLine("commit -m update-javadoc --allow-empty")
    .execCmdLine("push");
```

The tool offers many useful methods for executing standard DevOps commands like checking the workspace state, retrieving commit messages, and viewing current tags. For more details, visit the [Javadoc](https://jeka-dev.github.io/jeka/javadoc/dev/jeka/core/api/tooling/git/JkGit.html).

```java title="Conenient method calls"
List<String> tagNames = git.getTagsOnCurrentCommit();
String commit         = git.getCurrentCommit();
String messageToken   = git.extractSuffixFromLastCommitMessage("release:");
...
```

## [`JkVersionFromGit` class](https://github.com/jeka-dev/jeka/blob/master/dev.jeka.core/src/main/java/dev/jeka/core/api/tooling/git/JkGit.java)

This class infers project versions from Git information based on the current tag or branch.

- If the current commit is on a tag, the returned version is `tag-name`.
- If not on a tag, the returned version is `branch-name-SNAPSHOT`.

```java
JkVersionFromGit versionFromGit = JkVersionFromGit.of("v");  // consider only tags starting with 'v'
System.out.println(versionFromGit.getVersion());

JkProject project = getProject();
versionFromGit.handleVersioning(project);
```
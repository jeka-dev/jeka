# Git API

The Git API provides classes for conveniently working with Git in Java.
The classes are part of the [`dev.jeka.core.api.tooling.git` package](https://github.com/jeka-dev/jeka/tree/master/core/src/main/java/dev/jeka/core/api/tooling/git).

## [`JkGit` class](https://github.com/jeka-dev/jeka/blob/master/core/src/main/java/dev/jeka/core/api/tooling/git/JkGit.java)

The `JkGit` class extends `JkAbstractProcess`, as `JkProcess` does, which means it inherits methods that make process execution easier.

```java title="Sequence of Git calls"
JkGit git = JkGit.of(rootDir);
git
    .execCmdLine("add .")
    .execCmdLine("config user.name  jeka-bot")
    .execCmdLine("config user.email jeka-bot@github-action.com")
    .execCmdLine("commit -m update-javadoc --allow-empty")
    .execCmdLine("push");
```

The tool offers many useful methods for executing standard DevOps commands like checking the workspace state, retrieving commit messages, and viewing current tags. For more details, visit the [Javadoc](https://jeka-dev.github.io/jeka/javadoc/dev/jeka/core/api/tooling/git/JkGit.html).

```java title="Convenient method calls"
List<String> tagNames = git.getTagsOnCurrentCommit();
String commit         = git.getCurrentCommit();
String messageToken   = git.extractSuffixFromLastCommitMessage("release:");
```

## [`JkVersionFromGit` class](https://github.com/jeka-dev/jeka/blob/master/core/src/main/java/dev/jeka/core/api/tooling/git/JkVersionFromGit.java)

This class infers project versions from Git information based on the current tag or branch.

- If the current commit is on a tag and the workspace is clean, the returned version is the tag name, without the prefix.
- Otherwise, the returned version is `branch-name-SNAPSHOT`, or `short-commit-hash-SNAPSHOT` if there is no current branch (detached HEAD).

```java
JkVersionFromGit versionFromGit = JkVersionFromGit.of("v");  // consider only tags starting with 'v'
System.out.println(versionFromGit.getVersion());

versionFromGit.handleVersioning(project);
```
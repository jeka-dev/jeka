# Git KBean

<!-- header-autogen-doc -->

[`GitKBean`](https://github.com/jeka-dev/jeka/blob/master/core/src/main/java/dev/jeka/core/tool/builtins/tooling/git/GitKBean.java) provides command combos such as:

- Displaying a list of commit messages since the last tag.
- Pushing remote tags with guards to ensure the local workspace is clean.

## Usage

Display what has changed since the last release:
```shell
jeka git: lastTag lastCommitMessages
```

Release the current commit by tagging it:
```shell
jeka git: tagRemote
```
This displays the existing tags and prompts for the new one, which is then created on the current commit and pushed to `origin`. 
The command stops if there are uncommitted changes, or if the current branch is not in sync with its remote tracking branch.

Move the `latest` tag to the current commit:
```shell
jeka git: moveLatest
```
The existing `latest` tag is replaced, both locally and on `origin`.

!!! note
    `lastTag` and `lastCommitMessages` need at least one tag reachable from the current branch.

## Version from Git

To infer the project version from the current tag or branch, the Git KBean is not needed. Enable `gitVersioning` on the KBean 
handling the code:
```properties title="jeka.properties"
@project.gitVersioning.enable=true

# Optional, for tags as 'v1.3.1'
@project.gitVersioning.tagPrefix=v
```
Use `@base.gitVersioning` for the [`base` KBean](kbeans-base.md). If the current commit is tagged and the workspace is clean, the version 
is the tag without its prefix, also on a detached HEAD. Otherwise, it is `[branch]-SNAPSHOT` (`/` replaced by `-`), or 
`[short commit hash]-SNAPSHOT` if there is no current branch (detached HEAD).

To run other Git commands from Java, see the [Git API](api-git.md).

## Summary

<!-- body-autogen-doc -->



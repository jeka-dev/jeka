# App KBean

<!-- header-autogen-doc -->

[`AppKBean`](https://github.com/jeka-dev/jeka/blob/master/core/src/main/java/dev/jeka/core/tool/builtins/app/AppKBean.java) manages the installation, update, and discovery of applications from remote repositories or catalogs.

For a practical guide on running and installing applications from source, see the [Source-Runnable Applications tutorial](../tutorials/source-runnable-apps.md).

**Key Features**

- Installs applications from remote Git repositories.
- Supports JVM, Native, and Bundle runtime modes.
- Manages application updates and uninstallation.
- Facilitates application discovery via catalogs.
- Provides security through trusted source verification.

## Install Applications

```shell
jeka app: install repo=REPO_URL
```

JeKa may ask to trust the URL, to choose a version if the repository has tags, and to choose a name if neither `name` nor a catalog application is specified. 
These questions need a terminal: without it, the command fails. `repo=REPO_URL#1.2.0` is the same as `repo=REPO_URL tag=1.2.0`. Only tags can be installed, not branches.

To install without any question, as in a script or CI, trust the URL first, then pass the tag and the name:

```shell
jeka app: trustUrl url=github.com/my-org/
jeka app: install repo=https://github.com/my-org/my-app.git tag=1.2.0 name=my-app
```

Use `tag=@` to install the last commit of the default branch.

!!! note
    Add *~/.jeka/apps* to your `PATH` before installing applications. Otherwise, they are installed in the JeKa distribution directory, 
    and may be lost when switching versions (e.g. with SDKMAN!).

Applications can be installed in different runtime modes:

### Runtime Modes

- **JVM** (default): JVM application running on a JDK managed by Jeka. Binary is installed in `~/.jeka/apps` and available in PATH.
- **NATIVE**: Native executable compiled ahead-of-time using GraalVM.
- **BUNDLE**: Application bundled with a tailored JRE. JeKa opens the folder holding the bundle (a `.dmg` on macOS), for you to install it. macOS and Windows only.

### Using the `runtime` parameter

Install in native mode:
```shell
jeka app: install repo=REPO_URL runtime=NATIVE
```

Install as a bundle:
```shell
jeka app: install repo=REPO_URL runtime=BUNDLE
```

Examples:
Install *kill8* app as a JVM application:
```shell
jeka app: install repo=https://github.com/djeang/kill8 name=killport
```
or use the GitHub repo shorthand
```shell
jeka app: install repo=kill8@djeang
```

or use an alternative application name or runtime mode
```shell
jeka app: install repo=https://github.com/djeang/kill8 name=killport runtime=NATIVE
```

## List Installed Applications

```shell
jeka app: list
```
This displays the name of the applications, along with their repo, version/status, and runtime type.
```
App Name   │ Repo                                      │ Version  │ Status     │ Runtime │ 
calculator │ https://github.com/djeang/Calculator-jeka │ latest   │ up-to-date │ jvm     │ 
kill8      │ https://github.com/djeang/kill8           │ <master> │ up-to-date │ native  │ 
```

## Update Applications

```shell
jeka app: update name=kill8
```
The update depends on what is currently installed:

- A version tag (e.g. `1.2.0`): JeKa looks for higher version tags and proposes to update to the highest one.
- Any other tag (e.g. `latest`): the tag is kept, and its content is refreshed if it has changed.
- No tag: the application is updated to the latest commit.

## Uninstall Applications

```shell
jeka app: uninstall name=kill8
```

If no app has this name, the command fails, as `app: update` does. Add `force=true` to only get a warning, as in a cleanup script.

## Catalogs

Application catalogs are curated collections of Jeka applications that can be easily discovered and installed. Catalogs make it simple to share and distribute applications across teams or communities.

### Viewing Catalogs

Display all registered catalogs:
```shell
jeka app: catalog
```

This shows the catalog names, descriptions, and URLs sorted by repository.

Display applications in a specific catalog:
```shell
jeka app: catalog name=demo
```

This lists all applications available in the specified catalog, showing:

- Application name and description
- Application type (CLI, SERVER, SERVER-UI, etc.)
- Commands to run or install the application
- Available runtime modes (JVM, NATIVE, BUNDLE)

### Installing from Catalogs

Install an application using the catalog shorthand:
```shell
jeka app: install repo=cowsay@demo
```

The format is `appName@catalogName`. This is equivalent to providing the full repository URL.

### Creating a Catalog

To add a catalog, add the following lines to your `~/.jeka/global.properties` file:
```properties
jeka.catalog.xxx.repo=[http location or git repo url]
jeka.catalog.xxx.desc=[Description of the catalog]
```
where `xxx` stands for the name of your catalog.

The `jeka.catalog.xxx.repo` value can be:

1. A full URL to the catalog properties file
2. A GitHub organization name (looks for `jeka-catalog` repository)
3. A GitHub organization/repository path

**Valid examples:**
```properties
# GitHub organization - expects jeka-catalog repo at root
jeka.catalog.djeang.repo=djeang
jeka.catalog.djeang.desc=Personal applications

# GitHub organization/repository - expects jeka-catalog.properties at root
jeka.catalog.demo.repo=jeka-dev/demo
jeka.catalog.demo.desc=Demo applications

# Direct URL to catalog file
jeka.catalog.custom.repo=https://raw.githubusercontent.com/myorg/my-catalog/refs/heads/main/jeka-catalog.properties
jeka.catalog.custom.desc=Custom application catalog
```

### Catalog File Format

A catalog file is a properties file named `jeka-catalog.properties` containing application definitions:

```properties
# Application entries follow the pattern: app.<name>.<property>=<value>

app.cowsay.repo=https://github.com/jeka-dev/demo-cowsay
app.cowsay.desc=Java port of the Cowsay famous CLI.
app.cowsay.type=CLI
app.cowsay.native=true
app.cowsay.bundle=false

app.demo-springboot-angular.repo=https://github.com/jeka-dev/demo-project-springboot-angular
app.demo-springboot-angular.desc=Manage a list of users. Written in Spring Boot and Angular.
app.demo-springboot-angular.type=SERVER-UI
app.demo-springboot-angular.native=false
app.demo-springboot-angular.bundle=true
```

**Available application properties:**

- **`repo`** (required): Git repository URL of the application
- **`desc`** (required): Short description of the application
- **`type`** (optional): Application type (CLI, SERVER, SERVER-UI, LIBRARY, etc.)
- **`native`** (optional): Set to `true` if the app supports native compilation (default: false)
- **`bundle`** (optional): Set to `true` if the app supports bundled distribution (default: false)

### Built-in Catalogs

JeKa comes with built-in catalogs (`demo`, `djeang`, `thirdparty`), listed by `jeka app: catalog`. Additional catalogs can be registered
in `~/.jeka/global.properties`. A project's `jeka.properties` is not read.


## Security

Repositories from `github.com/jeka-dev/` are trusted by default. 
See [Trust and Security](../tutorials/source-runnable-apps.md#trust-and-security) for the different ways to trust other URLs, and the behavior without a terminal.

Trusted URL prefixes are stored in the `jeka.app.url.trusted` property, located in the `~/.jeka/global.properties` file.  
You can adjust this property later to make it more or less restrictive, or add a prefix by running 
`jeka app: trustUrl url=github.com/djeang/`. Prefixes are separated by spaces.  
The check compares whole path segments of the Git URL, after removing the protocol, user and tag: `github.com/djeang` trusts 
`github.com/djeang/xxx`, but not `github.com/djeang-other/xxx`. URLs containing `..`, `//`, `%`, `\` or spaces are never trusted.

Prefer entries naming a single repository (`github.com/djeang/kill8`).

Example: `jeka.app.url.trusted=github.com/djeang/` will trust URLs formed as:

  - `https://github.com/djeang/xxx`
  - `https://my-user-name@github.com/djeang/xxx`
  - `git@github.com:djeang/xxx.git`
  - `ssh://git@github.com/djeang/xxx.git`

## Summary

<!-- body-autogen-doc -->



# CLI Installation

You don't need to install a JDK: JeKa downloads the version required by each project (about 200 MB, once). 
If a project does not specify any, JeKa uses the JDK set in `JAVA_HOME`, or downloads JDK 21.

## Windows

```shell
iex "& { $(iwr -useb https://jeka.dev/install.ps1) } install check"
```

## SDKMAN!

```shell
sdk install jeka
```
To [install applications](tutorials/source-runnable-apps.md#desktop-installation) with JeKa, also add *~/.jeka/apps* to your `PATH`. 
Otherwise, they are installed in the directory of the current JeKa version.

## MacOS and Linux

```shell
curl -sL https://jeka.dev/install.sh | $(echo $0) -s - install check
```

`$(echo $0)` stands for your current shell. If the command fails (e.g. `-zsh: command not found`), replace it with `bash`:
```shell
curl -sL https://jeka.dev/install.sh | bash -s - install check
```

The script installs JeKa in *~/.jeka/bin*, and adds this directory to the `PATH` in your shell profile (e.g. *~/.zshrc*). 
To install applications with JeKa, also add *~/.jeka/apps* to your `PATH`.

!!! note

    JeKa requires *bash*, *curl*, and *unzip*, usually available on macOS and Linux.
    On Ubuntu, install tools with: 
    ```shell
    apt-get update && apt-get install -y curl unzip git gcc zlib1g-dev
    ```


## Manual installation

The manual installation is straightforward and may help when script installation fails.

- Download latest JeKa distrib from [maven central](https://central.sonatype.com/artifact/dev.jeka/jeka-core/versions)
  and download file named *jeka-core-xxx-distrib.zip*.
- Unzip the content of the zip file and copy the content of the `bin` directory to `[USER HOME]/.jeka/bin`. 
- Add `[USER HOME]/.jeka/bin` and `[USER HOME]/.jeka/apps` to your `PATH` environment variable.
- On macOS and Linux, make the scripts executable: `chmod +x ~/.jeka/bin/jeka ~/.jeka/bin/jeka-update`.
- On Windows, edit the user `PATH` in *System Properties > Environment Variables*, then open a new terminal.

## Post Install

Installation via scripts may include a sanity check that triggers a JDK download (because of the `check` argument passed to the install script).

Once installed, you will rarely need to upgrade, as JeKa will execute the JeKa version specified in
the application to run.
However, you'll be able to upgrade your base install by executing:
```bash
jeka-update
```

Open a new terminal session and execute the following command to access JeKa help:
```bash
jeka --help
```

Next: [Quick Start](quick-start.md).

## Uninstall

Delete *~/.jeka*, which contains JeKa, its cache, installed applications and *global.properties*, then remove it from your `PATH` 
(in your shell profile, or in the Windows environment variables). With SDKMAN!, also execute `sdk uninstall jeka <version>`.

## Docker Image - Zero Install

JeKa can be executed using the Docker image [jekadev/jeka](https://hub.docker.com/r/jekadev/jeka).

To do this, execute: 

- Linux/macOS: `docker run -v $HOME/.jeka/cache4c:/cache -v .:/workdir jekadev/jeka [JEKA ARGUMENTS]`
- Windows PowerShell: `docker run -v ${HOME}\.jeka\cache4c:/cache -v ${PWD}:/workdir jekadev/jeka [JEKA ARGUMENTS]`
- Windows cmd: `docker run -v %USERPROFILE%\.jeka\cache4c:/cache -v %cd%:/workdir jekadev/jeka [JEKA ARGUMENTS]`

!!! note

    `-v $HOME/.jeka/cache4c:/cache` 
    
    Tells JeKa to use a specific cache when running with a container, as JDK or other tools 
    cached by JeKa may differ from the ones used by the host system.
    
    `-v .:/workdir` 
    
    Lets JeKa operate in the current directory of the host machine.

    `[JEKA ARGUMENTS]` 
    
    Stands for regular JeKa arguments you would pass to JeKa command line such as `project: pack` or `--help`.






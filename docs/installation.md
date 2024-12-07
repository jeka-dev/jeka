# Installation

## IntelliJ Plugin

For a pure IDE experience, you don't need to install CLI, just install the [IntelliJ Plugin](https://plugins.jetbrains.com/plugin/24505-jeka).

## CLI for Windows

The CLI lets you run remote apps/scripts and create new projects.

Execute in PowerShell :
```shell
iex "& { $(iwr -useb https://jeka.dev/install.ps1) } install check"
```

## CLI for MacOS and Linux

Execute :
```shell
curl -sL https://jeka.dev/install.sh | $(echo $0) -s - install check
```

!!! note
    JeKa requires *bash*, *curl*, and *unzip*, usually available on macOS and Linux.

    On Ubuntu, install missing tools with: `apt-get update && apt-get install -y curl unzip`.


## Manual CLI installation

The manual installation is straightforward and may help when script installation fails.

- Download latest JeKa distrib from [maven central](https://central.sonatype.com/artifact/dev.jeka/jeka-core/versions)
  and download file named *jeka-core-xxx-distrib.zip*.
- Unzip the content of the zip file and copy the content of 'bin' directory to an arbitrary directory ([USER HOME]/.jeka/bin for instance)
- Add this directory to your PATH environment variable.

## Post Install

Installation via script may include a sanity check that triggers a JDK download (cause of the 'check' argument passed to the install script)

Once installed, you will rarely need to upgrade, as JeKa will execute the JeKa version specified in
the application to run.
However, you'll be able to upgrade your base install by executing `jeka-update` command.

Open a new terminal session and execute `jeka --help` to access JeKa help.

## Docker Image - Zero Install

JeKa can be executed using the Docker image [jekadev/jeka](https://hub.docker.com/r/jekadev/jeka). This can be useful to force build execution on a 
Linux host, which is mandatory to produce Java native image for Linux and containers.

For this, execute : 

- Linux/Macos        : `docker run -v $HOME/.jeka/cache4c:/cache -v .:/workdir jekadev/jeka [JEKA ARGUMENTS]`
- Windows Powershell : `docker run -v ${HOME}\.jeka\cache4c:/cache -v ${PWD}:/workdir jekadev/jeka [JEKA ARGUMENTS]`
- Windows cmd        : `docker run -v %USERPROFILE%\.jeka\cache4c:/cache -v %cd%:/workdir jekadev/jeka [JEKA ARGUMENTS]`

!!! notes

    `-v $HOME/.jeka/cache4c:/cache` 
    
    Tells jeka to use a specific cache when running with container, as JDK or other tools 
    cached by Jeka may differ from the ones used by the host system.
    
    `-v .:/workdir jeka --version` 
    
    Lets Jeka operate in the current directory of the host machine

    `[JEKA ARGUMENTS]` 
    
    Stands for regular jeka arguments you would pass to jeka command line as `project: back' or '--help'.






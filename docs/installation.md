# Installation

!!! note
    You don't need to install JeKa to build projects that have *JeKa* shell scripts in their Git repository.
    
    Nevertheless, this is convenient to have it installed for invoking it easily from everywhere.


## MacOS and Linux

Execute :
```shell
curl -sL https://jeka.dev/install.sh | $(echo $0) -s - install check
```

!!! note
    For installing and running JeKa, *bash*, *curl*, and *unzip* are required. This is generally the case
    for macOS and most Linux distributions. If any of these tools are missing, you can install them on Ubuntu
    by executing `apt-get update && apt-get install -y curl unzip`.

## Windows

Execute in PowerShell :
```shell
iex "& { $(iwr -useb https://jeka.dev/install.ps1) } install check"
```

## Manual installation

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

For this, execute : `docker run -docker run -v $HOME/.jeka/cache4c:/cache -v .:/workdir jekadev/jeka [JEKA ARGUMENTS]`.

- `-v $HOME/.jeka/cache4c:/cache` tells jeka to use a specific cache when running with container, as JDK or other tools 
cached by Jeka may differ from the ones used by the host system.

- `-v .:/workdir jeka --version` lets Jeka operate in the current directory of the host machine

- `[JEKA ARGUMENTS]` stands for regular jeka you would pass to jeka command line as `project: back' or '--help'.






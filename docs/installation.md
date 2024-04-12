# Installation

!!! note
    You don't need to install JeKa to build projects hosting *jeka* scripts in their Git repository.
    
    Nevertheless, this is practical to have it installed for invoking it easily from everywhere.


## MacOS and Linux

For installing and running, JeKa needs only *curl* and *unzip* be installed on the system.

Execute :
```shell
curl -s https://raw.githubusercontent.com/jeka-dev/jeka/0.11.x/dev.jeka.core/src/main/shell/jekau | $(echo $0) -s - setup
```
This installs JeKa, and launches a sanity checks that triggers a JDK download.

Once installed, you will rarely need to upgrade, as JeKa will execute the JeKa version specified in
the application to run.
However, you'll be able to upgrade your base install by executing `jeka-install` command.

## Windows

For installing and running, JeKa needs only *curl* be installed on the system.

## Manual installation

The manual installation is straightforward and may help when script installation fails.

- Download latest JeKa distrib from [maven central](https://central.sonatype.com/artifact/dev.jeka/jeka-core/versions)
  and download file named *jeka-core-xxx-distrib.zip*.
- Unzip the content of the zip file and copy the content of 'bin' directory to an arbitrary directory ([USER HOME]/.jeka/bin for instance)
- Add this directory to your PATH environment variable.

## Sanity check

Once installed, you can open a new terminal session and execute `jeka --help` to see runtime details.





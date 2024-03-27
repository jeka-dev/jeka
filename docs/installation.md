# Installation

You don't need to install JeKa to use it as a build tool, as is is generally present in 
the project base directory.

Nevertheless, you might need to install it for the following use-cases :

- Scaffold (create skeleton) of new projects from your local machine.
- Execute remote scripts/applications located of filesystem or *git* repo.
- Use shorter command `jeka` instead of `./jeka`or `.\jeka.ps`.

Once installed, you will rarely need to upgrade, as JeKa will execute the JeKa version specified in
the application to run.
However, you'll be able to upgrade your base install by executing `jekau` command.

## MacOS and Linux

For installing and running, JeKa needs only *curl* and *unzip* be installed on the system.

Execute :
```shell
curl -s https://raw.githubusercontent.com/jeka-dev/jeka/0.11.x/dev.jeka.core/src/main/shell/jekau | $(echo $0) -s - setup
```
This installs JeKa, and launches a sanity checks that triggers a JDK download.

## Windows

For installing and running, JeKa needs only *curl* be installed on the system.

## Manual installation

The manual installation is straightforward and may help when script installation fails.

- Download latest JeKa distrib from [maven central](https://central.sonatype.com/artifact/dev.jeka/jeka-core/versions)
  and download file named *jeka-core-xxx-distrib.zip*.
- Unzip the content of the zip file to an arbitrary directory
- Add this directory to your path.

## Sanity check

Once installed, you can open a new terminal session and execute `jeka --help` to see runtime details.





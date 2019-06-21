[![Build Status](https://travis-ci.org/jerkar/jeka.svg?branch=master)](https://travis-ci.org/jerkar/jeka)
[![Maven Central](https://img.shields.io/maven-central/v/dev.jeka/jeka-core.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22dev.jeka%22%20AND%20a:%22jeka-core%22) <br/>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
<img src="http://jeka.dev/images/logo-whole-bg.jpg" width='420' height='420' align='middle'/>

# What is Jeka

<strong>Jeka</strong>(formerly Jerkar) is a complete **Java build system** ala _Ant_, _Maven_, _Gradle_ or _Buildr_ using only Java code to automate builds or tasks.

Forget about verbose Poms and rigid Maven structure. Get rid of Gradle scripting.  

Enjoy all the engineering power you are comfortable with : Java code, IDE, 3rd party libs, binary repositories, .... 

Model, refactor, run, debug, reuse automation assets across tasks and projects. Exactly as you do with your regular Java code. 

Also, __Jeka conventions and plugin mechanism are so powerful__ that it can perform pretty exotic tasks without needing a single line of code/configuration. 

For example `jeka java#pack jacoco# sonar#run -sonar#host.url=http://myserver/sonar`
performs a complete build running unit tests under Jacoco coverage tools and performs SonarQube analysis on a Java project free 
of any build-code / configuration / script. 

# News 

_Jerkar_ has been rebranded to _Jeka_. Maven groupId and artifactId has moved to `dev.jeka.jeka-core`.
It has a completely re-designed API based on Java7 nio and Java8 lambdas providing a much cleaner/polished design 
and leaner user code.

Last addition was about deploying on Maven central though a modern release process (version numbering based on Git instead of being hardcoded).
Jeka now uses these features to release itself.

Please visit [relelease note](https://github.com/jerkar/jeka/blob/master/release-note.md) and [issues](issues) for roadmap.

# Get Jeka

* Snapshots : https://oss.sonatype.org/content/repositories/snapshots/dev/jeka/jeka-core/
* Releases : https://repo1.maven.org/maven2/dev/jeka/jeka-core/

The distribution is the file named jeka-core-x.x.x-distrib.zip. 

# How to use Jeka

Jeka is designed to be easy to master for Java developers. It is easy to figure out how it works by knowing few 
concepts and navigate in source code.

That said, documentation is needed for a starting point.

Visit following pages according your expectation :
* [Getting Started](dev.jeka.core/src/main/doc/Getting%20Started.md)
* [Reference Guide](dev.jeka.core/src/main/doc/Reference%20Guide)
* [Frequently Asked Questions](dev.jeka.core/src/main/doc/FAQ.md)
* [Javadoc](https://jeka.dev/docs/javadoc)
* [Working examples](https://github.com/jerkar/working-examples)

# External plugins

Jeka comes with plugins out of the box. These plugins covers the most common points a Java developer need to address 
when building a project. This includes plugins for IDE metadata generation (IntelliJ, Eclipse), dependency management, git, , java project building, testing, PGP signing, binary repositories, Maven interaction, scaffolding, sonarQube and web archives.

Nevertheless, Jeka is extensible and other plugins exist outside the main distib among :
* [Springboot Plugin](https://github.com/jerkar/springboot-plugin)
* [Protobuf Plugin](https://github.com/jerkar/protobuf-plugin)

# Community

You can ask question using regular using [this repository issues](https://github.com/jerkar/jerkar/issues).

You can also use direct emailing for questions and support : djeangdev@yahoo.fr

A twitter account also exist : https://twitter.com/djeang_dev

# How to build Jeka

Jeka is made of following projects :
* dev.jeka.core : complete Jeka project
* dev.jeka.samples : A sample project with several build classes to illustrate how Jeka can be used in different ways
* dev.jeka.depender-samples : A sample project depending on the above sample project to illustrate multi-project builds. 
These sample projects are also used to run some black-box tests

Jeka builds itself. To build Jeka full distrib from sources, the simpler is to use your IDE.

Once distrib created, add the distrib folder to your PATH environment variable.

## Build Jeka from Eclipse

* Clone this repository in Eclipse. Project is already configured ( *.project* and *.classpath* are stored in git). 
* Make sure the project is configured to compile using a JDK8 and not a JRE.
* Run `dev.jeka.core.CoreBuild` class main method. This class is located in *jeka/def* folder. 
* This creates the full distrib in *dev.jeka.core/jeka/output/distrib* folder

## Build Jeka from IntelliJ

* Clone this repository into IntelliJ. Project is already configured (.iml and modules.xml are stored in git).
* Make sure the project is configured with a JDK8.
* Run `dev.jeka.core.CoreBuild` class main method. This class is located in *jeka/def* folder, inside *dev.jeka.core* project.
* This creates the full distrib in *dev.jeka.core/jeka/output/distrib* folder

## How to Release ?

Release is done automatically by Travis at each git push. If their is no tag on the current commit then it goes to a 
SNAPSHOT deploy on OSSRH. If there is a tag, it goes to a publish on Maven central.

To realy deploy to Maven central, a manual action it still needed to [close/release repository](https://oss.sonatype.org).

To create a tag conveniently, just execute `jeka git#tagRemote` from your console and answer to the prompt. 




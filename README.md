![Logo of Jerkar](https://github.com/jerkar/jerkar/blob/master/doc/jerkar.png)

Jerkar is a complete **Java** built system ala _Ant_, _Maven_ or _Gradle_ but using **Java only** to describe builds : no XML, no foreign language.
Breaking a common belief, it makes proof that Java is perfectly suitable in this domain.

It is primarily intended to build project written in Java language but can also be used for any task execution purpose.

# Motivation
So far, for building their projects, java developers generally use an XML based (_Ant_, _Maven_) or a foreign language DSL (_Gradle_, _Rake_, _SBT_, ...) tool.
They just can't use Java to create organization scalable builds. **Jerkar** fills this gap. In jerkar build scripts are plain old java classes, bringing great benefits :

* power, flexibility and robustness of Java
* compilation, code-completion and debug facilities provided by your IDE without installing 3rd party plugins/tools
* lightness, simplicity, speed : in essence, Jerkar engine simply performs method invocations on your build class
* no extra language or XML soup to master, no required configuration file
* any Java 3rd party libraries without needing to wrap it in a plugin or a specific component
* complexity tackling the same way you do for regular code (ie utility classes, SoC, inheritance, composition,...) 
* fluent and compact syntax documented through Javadoc

Additionally the following features were missing from mainstream existing tools :
* run pluggable extra features (test coverage, special packaging, static code analysis,...) without editing the build file
* write nothing-at-all for building simple/standard projects (just relying on convention and/or IDE meta-data files, even to launch static analysis tools or generate ear/war files)

See [how Jerkar adresses Java shortcomings](doc/readme-parts/java-adress-shortcomings.md) 

# Main features
Jerkar provides what a self respecting modern, enterprise scale, build system should and more :
* provides both APIs and a command line tool
* multi-level configuration (system, user, project, command line)
* powerfull dependency management (back-ended by Ivy so compatible with Maven repositories)
* publication on Ivy or Maven repositories
* multi-project support
* powerfull fluent API to manipulate files, perform  compilations, tests, archives and all build related stuff
* choice between free form builds (ala Ant) and enforced build templates (ala Maven)
* hierarchical log output monitoring execution time for each step
* pluggable architecture
* scaffolding to get projects started quickly
* ability to get information from naming convention and Eclipse files, so in simpler cases you won't need to write script at all (even to test coverage or perform SonarQube analysis) !!!

# How it works ?
Jerkar builds with itself and this is not a trivial case cause it involves multi-project, distribution crafting, 
manifest updating and Maven publication. So you may look here to get deeper understanding: [how to build Jerkar](doc/readme-parts/build-jerkar.md) and  See [how to quick start](doc/readme-parts/quick-start.md)

But to start simple, the project [org.jerkar.script-samples](org.jerkar.script-samples) holds some build script [examples](org.jerkar.script-samples/build/spec/org/jerkar/scriptsamples). 
Just know that in Jerkar, build scripts (build classes) are supposed to be stored in `[project root]/build/spec` folder,
and that Jerkar compile everything under this folder prior to execute the first build class found 
(you can however specify the executed build class by mentioning `-buildClass=MyClassSimpleName` option in Jerkar command line).

#### Explicit build
___
This is an academic script for a educational purpose, normally we won't specify projectName, groupName or version
as they are supposed to be deducted from conventions.
```java
public class BuildSampleClassic extends JkJavaBuild {
	
	@Override  // Optional : needless if you respect naming convention
	public String projectName() {
		return "script-samples";
	}
	
	@Override  // Optional : needless if you respect naming convention
	public String groupName() {
		return "org.jerkar";
	}
	
	@Override   // Optional : needless if you get the version from your SCM or version.txt resource
	protected JkVersion defaultVersion() {
		return JkVersion.named("0.1-SNAPSHOT");
	}
	
	@Override  // Optional :  needless if you use only local dependencies
	protected JkDependencies dependencies() {
		return JkDependencies.builder()
			.on(GUAVA, "18.0")   // Popular modules are available as Java constant
			.on(JERSEY_SERVER, "1.19")
			.on("com.orientechnologies:orientdb-client:2.0.8")
			.on(JUNIT, "4.11").scope(TEST)
			.on(MOCKITO_ALL, "1.9.5").scope(TEST)
		.build();
	}
}
```
The [complete code source](org.jerkar.script-samples/build/spec/org/jerkar/scriptsamples/BuildSampleClassicExplicit.java)

#### Classic build
___
By respecting conventions (project folder named as _groupName_._projectName_ so `org.jerkar.script-samples`)
and leveraging default (version is read from the version.txt resource located in the same package as the build class), the following script is equivalent :

```java
public class BuildSampleClassic extends JkJavaBuild {
	
	@Override  // Optional :  needless if you use only local dependencies
	protected JkDependencies dependencies() {
		return JkDependencies.builder() 
			.on(GUAVA, "18.0")  
			.on(JERSEY_SERVER, "1.19")
			.on("com.orientechnologies:orientdb-client:2.0.8")
			.on(JUNIT, "4.11").scope(TEST)
			.on(MOCKITO_ALL, "1.9.5").scope(TEST)
		.build();
	}	
}
```
The [complete code source](org.jerkar.script-samples/build/spec/org/jerkar/scriptsamples/BuildSampleClassic.java)

On the command line, under root project folder :
- type `jerkar doDefault` => clean, compile, run tests and pack (produce jar and source jar), as mentioned in the `JkJavaBuild#doDefault` method
- type `jerkar` => will do exactly the same thing : when no method is specified, `doDefault`is run
- type `jerkar -fatJar -forkTests` => do the same but also will produce a fat jar 
(jar file containg all the runtime dependencies) and run unit tests in a forked process.
- type `jerkar jacoco#` => will do as `jerkar` but activating jacoco plugin so the run will produce 
a code coverage report usable by tools as SonarQube
- type `jerkar doDefault sonar#verify jacoco#` => do the default + execute the method `verify` method located in the `sonar`plugin 
(class [JkBuildPluginSonar](org.jerkar.plugins-sonar/src/main/java/org/jerkar/plugins/sonar/JkBuildPluginSonar.java)).
Analysis is launched on a local SonarQube server unless you specify specific Sonar settings. Sonar will leverage of jacoco report.
- type `jerkar doDefault sonar#verify -sonar.host.url=http://my.sonar.host:8080` to specify a SonarQube server host. `-myProp=value` is the way
in Jerkar to pass parameters (called options) through the command line.

_Simpler than you was accustomed to, He ??_

Note that there is other way for passing option than using the command line. You can define them at three other level :
- Coded in the build script itself
- In option.properties file located in Jerkar install directory
- In option.propertyiesfile located in [user home]/.jerkar directory

Note that in the complete source code, you'll find a `main` method. It's mainly intended to run the whole script friendly in your favorite IDE.
It's even faster cause you skip the script compile phase.
___
#Status

The documentation is at its very early stage but the code is yet pretty close to completion for a first release. 
I mainly need help for further testing, writing documentation, polishing the API... and getting some feedback of course.

See [how to build Jerkar](doc/readme-parts/build-jerkar.md)

See [how to quick start](doc/readme-parts/quick-start.md)

See [an example on how jerkar build itself](doc/readme-parts/example-jerkar-core.md)
    
        
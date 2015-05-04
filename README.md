![Logo of Jerkar](https://github.com/jerkar/jerkar/blob/master/doc/jerkar.png)

Jerkar is a complete **Java** built system ala _Ant_, _Maven_ or _Gradle_ but using **Java only** to describe builds : no XML, no foreign language.
Breaking a common belief, it makes proof that Java is perfectly suitable in this domain.

It is primarily intended to build project written in Java language but can also be used for any task execution purpose.

# Motivation
So far, for building their projects, java developers generally use an XML based (_Ant_, _Maven_) or a foreign language DSL (_Gradle_, _Rake_, _SBT_, ...) tool.
They just can't use Java to create organization scalable builds. **Jerkar** fills this gap. In jerkar build scripts are plain old java classes, bringing great benefits :

* power, flexibility and robustness of Java
* compilation, code-completion and debug facilities provided by your IDE without installing 3rd party plugins/tools
* lightness, simplicity, transparency, speed : in essence, Jerkar engine simply performs method invocations on your build class
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

Let's assume you have already [installed Jerkar](doc/readme-parts/quick-start.md).

To start concrete, the project [org.jerkar.script-samples](org.jerkar.script-samples) holds some build script [examples](org.jerkar.script-samples/build/spec/org/jerkar/scriptsamples). 
Just know that in Jerkar, build definitions are plain Java class sources. They are stored in `[project root]/build/def` folder,
and are compiled on the fly by Jerkar in `[project root]/build/output/def-bin` folder.

Normally there is a single build definition by project but it is possible to have more. To precise a given build definition to run,
 mention `-buildClass=MyDefinitionSimpleClassName` option in Jerkar command line. 

With Jerkar you can write free form build definition (ala ANT) or templated ones (ala maven).

#### Ant style build

```
public class AntStyleBuild extends JkBuild {
	
	File src = baseDir("src");
	File buildDir = baseDir("build");
	File classDir = new File(buildDir, "classes");
	File jarFile = new File(buildDir, "jar/" + this.baseDir().root().getName() + ".jar");
	String className = "my.mainClass";
	JkClasspath classpath = JkClasspath.of(baseDir().include("libs/*.jar"));
	File reportDir = new File(buildDir, "junitRreport");
	
	@Override
	public void doDefault() {
		clean();run();
	}
	
	public void compile() {
		JkJavaCompiler.ofOutput(classDir).withClasspath(classpath).andSourceDir(src).compile();
		JkFileTree.of(src).exclude("**/*.java").copyTo(classDir);
	}
	
	public void jar() {
		compile();
		JkManifest.empty().addMainClass("my.main.RunClass").writeToStandardLocation(classDir);
		JkZipper.of(classDir).to(jarFile);
	}	
	
	public void run() {
		jar();
		JkJavaProcess.of(jarFile).andClasspath(classpath).runSync();
	}
	
	public void cleanBuild() {
		clean();jar();
	}
	
	public void junit() {
		jar();
		JkUnit.ofFork(classpath.and(jarFile))
				.withClassesToTest(JkFileTree.of(classDir).include("**/*Test.class"))
				.withReportDir(reportDir)
				.withReport(JunitReportDetail.FULL)
				.run();
	}
	
	public static void main(String[] args) {
		new AntStyleBuild().doDefault();
	}
	
}```

[complete code source for this build](org.jerkar.script-samples/build/spec/org/jerkar/scriptsamples/AntStyleBuild.java)

So now, we can execute Jerkar script the following way :
- write a main method in your script and launch it within your IDE.
- execute the `org.jerkar.JkMain` method in your IDE but with the root of your project as working directory. In this mode you
can pass arguments as you would do with the command line.
- executing a command line in a shell (or on a build server)  

To execute command line, open a shell and go under the project root directory. From there you can :
- execute `jerkar doDefault` => instantiate `JkJavaBuild``and invoke the `doDefault` method.
- execute `jerkar` => do the same, the `doDefault` method is invoked when none is specified
- execute `jerkar clean junit`=> instantiate `JkJavaBuild``and invoke the `clean` then `junit` method.


#### Maven style build
___
For Java project you may directly extend [JkJavaBuild template](org.jerkar.core/src/main/java/org/jerkar/builtins/javabuild/JkJavaBuild.java) 
so standard methods are already implemented. All you need is to implement what is specific.

This example is an academic script for a illustration purpose. Most these settings can be omitted 
by following naming convention or setting Jerkar at global level.

```java
public class MavenStyleBuild extends JkJavaBuild {
	
	@Override
	// Optional : needless if you respect naming convention
	public JkModuleId moduleId() {
		return JkModuleId.of("org.jerkar", "script-samples");
	}

	@Override
	// Optional : needless if you get the version from your SCM or version.txt
	// resource
	protected JkVersion defaultVersion() {
		return JkVersion.ofName("0.3-SNAPSHOT");
	}

	@Override
	// Optional : needless if you use only local dependencies
	protected JkDependencies dependencies() {
		return JkDependencies
				.builder()
				.on(GUAVA, "18.0")
				// Popular modules are available as Java constant
				.on(JERSEY_SERVER, "1.19")
				.on("com.orientechnologies:orientdb-client:2.0.8")
				.on(JUNIT, "4.11").scope(TEST).on(MOCKITO_ALL, "1.9.5")
				.scope(TEST).build();
	}
	
	@Override
	// Optional :Maven central by default. You can also set it in in 
	// your shared [USER DIR]/.jerkar/options.propereties file  
	protected JkRepos downloadRepositories() {
		return JkRepos.of(JkRepo.maven("http://my.repo1"), JkRepo.mavenCentral());
	}
	
	@Override
	// Optional : You can set it in in 
	// your shared [USER DIR]/.jerkar/options.propereties file
	protected JkPublishRepos publishRepositories() {
		return JkPublishRepos.ofSnapshotAndRelease(
				JkRepo.maven("http://my.snapshot.repo"), 
				JkRepo.ivy("http://my.release.repo"));
	}

}
```

[complete code source for this build](org.jerkar.script-samples/build/spec/org/jerkar/scriptsamples/MavenStyleBuild.java)


#### Tiny style build
___
If you follow best practices by respecting conventions (project folder named as _groupName_._projectName_ so `org.jerkar.script-samples`)
and store global settings (as repositories) to shared property files the above script is reduced to :

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
[complete code source for this build](org.jerkar.script-samples/build/spec/org/jerkar/scriptsamples/BuildSampleClassic.java)

So now, we can execute Jerkar script the following way :
- write a main method in your script and launch it within your IDE (see complete source code)
- execute the `org.jerkar.JkMain` method in your IDE but sing the root of your project as working directory. In this mode you
can pass arguments as you would do with the command line.
- executing a command line in a shell (or on a build server)  

To execute command line, open a shell and go under the project root directory. From there you can :
- execute `jerkar doDefault` => invoke the `JkJavaBuild#doDefault` method which lead in clean, compile, compile tests, run tests and pack (produce jar and source jar).
- execute `jerkar -fatJar=true -forkTests=true` => do the same but inject the `true` value to `JkJavaBuild#fatJar` and `JkJavaBuild#forkTests` fields. It leads in producing a fat-jar 
(jar file containg all the runtime dependencies) and running unit tests in a forked process.
- execute `jerkar -fatJar=true -forkTests=true` => do the same, when field values are not mentioned, Jerkar uses a default value (true for boolean fields)

- execute `jerkar jacoco#` => will instantiate the [jacoco plugin](org.jerkar.plugins-jacoco/src/main/java/org/jerkar/plugins/jacoco/JkBuildPluginJacoco.java) and bind it to the `BuidSampleClassic` instance. This plugin alter the `JkJavaBuild#unitTest` method 
in such a way that tests are run with Jacoco to produce a test coverage report. '#' is the mark of plugin in Jerkar command line.
- execute `jerkar jacoco# -jacoco#produceHtml` => will do the same but also set the `JkBuildPluginJacoco#produceHtml`field to `true`. It leads in producing 
an html report along the standard jacoco.exec binary report

- execute `jerkar doDefault sonar#verify jacoco#` => do the default + execute the method `verify` method located in the [sonar plugin] (org.jerkar.plugins-sonar/src/main/java/org/jerkar/plugins/sonar/JkBuildPluginSonar.java).
Analysis is launched on a local SonarQube server unless you specify specific Sonar settings. Sonar will leverage of jacoco report.
- execute `jerkar doDefault sonar#verify -sonar.host.url=http://my.sonar.host:8080` to specify a SonarQube server host. `-myProp=value` is the way
in Jerkar to pass parameters (called options) through the command line.

If you want the full method along available options on any build, simply type `jerkar help` and/or `jerkar helpPlugins`.

Note that there is other way for passing option than using the command line. You can define them at three other level :
- Coded in the build script itself
- In option.properties file located in Jerkar install directory
- In option.properties file located in [user home]/.jerkar directory

Note that in the complete source code, you'll find a `main` method. It's mainly intended to run the whole script friendly in your favorite IDE.
It's even faster cause you skip the script compile phase.


#### Parametrized build
___
You can set parameter in the build script itself and add your own custom parameters.. 
The following example define three possible sonar servers to run analysis on. It also forces the sonar project branch.
````java
public class BuildSampleSonarParametrized extends JkJavaBuild {
	
	@JkOption("Sonar server environment")
	protected SonarEnv sonarEnv = SonarEnv.DEV;
	
	@Override
	protected void init() {
		JkBuildPluginSonar sonarPlugin = new JkBuildPluginSonar()
			.prop(JkSonar.HOST_URL, sonarEnv.url)  // set one of the predefined host
			.prop(JkSonar.BRANCH, "myBranch");  // set the project branch
		this.plugins.activate(sonarPlugin);
	}
	
	@Override  
	protected JkDependencies dependencies() {
		return JkDependencies.builder()
			.on(GUAVA, "18.0")  
			.on(JUNIT, "4.11").scope(TEST)
		.build();
	}
	
	@Override
	public void doDefault() {
		clean();compile();unitTest();
		verify(); 
	}
	
	public enum SonarEnv {
		DEV("dev.myhost:81"),
		QA("qa.myhost:81"),
		PROD("prod.myhost:80");
		
		public final String url;
		
		SonarEnv(String url) {
			this.url = url;
		}
	}
}
``` 
[complete code source for this build](org.jerkar.script-samples/build/spec/org/jerkar/scriptsamples/BuildSampleSonarParametrized.java)
The Sonar plugin is activated programatically in the script so it is not required anymore to mention it in the build script.
So `jerkar` alone performs a clean, compile, test and sonar analysis on the default sonar environment (DEV).
`jerkar -sonarEnv=PROD`run it upon the the PROD environment.  

# How to build Jerkar ?

Jerkar builds with itself and this is not a trivial case cause it involves multi-project, distribution crafting, 
manifest updating and Maven publication. So you may look here to get deeper understanding: [how to build Jerkar](doc/readme-parts/build-jerkar.md) and  See [how to quick start](doc/readme-parts/quick-start.md)

# Status

The documentation is at its very early stage but the code is yet pretty close to completion for a first release. 
I mainly need help for further testing, writing documentation, polishing the API... and getting some feedback of course.

See [how to build Jerkar](doc/readme-parts/build-jerkar.md)

See [how to quick start](doc/readme-parts/quick-start.md)

See [an example on how jerkar build itself](doc/readme-parts/example-jerkar-core.md)
    
        
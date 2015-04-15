![Logo of Jerkar](https://github.com/jerkar/jerkar/blob/master/doc/jerkar.png)

Jerkar is a complete built system ala Maven or Gradle but using only a **Java** internal DSL to describe builds : no XML, no foreign language.
It is intended to build project written in Java language but can also be used for any task execution purpose.

# Motivation
So far, for building their projects, java developers generally use an XML based (**Ant**, **Maven**) or a foreign language DSL (**Gradle**, **Rake**, **SBT**, ...) tool.
They just can't use **Java** to create organization scalable builds. **Jerkar** purposes to fill this gap.
      
Yet, using Java for building a Java based project brings quite valuable benefits :
* You don't have to learn an extra language or XML soup just for build purpose : get higher cohesion and lower cognitive load
* You leverage directly the power and flexibility of Java
* You leverage compilation, code-completion and debug facilities provided by your IDE without installing 3rd party plugins/tools. For static typed language as Java, it notably brings robustness to your builds
* Your builds can benefit from any libraries without needing to wrap it in a plugin or a specific component
* You can master build complexity the same way you do for regular code (ie utility classes, SoC, inheritance, composition,...) 
* Using fluent style internal DSL, syntax is much more concise and explicit than an XML description
* It's easier to dig into the build engine to investigate on behavior as builds are in essence, only API calls

Additionally the following features were missing from mainstream existing tools :
* Run pluggable extra features (test coverage, special packaging, static code analysis,...) without editing the build file
* Write nothing-at-all for building simple/standard projects (just relying on convention and/or IDE meta-data files)

## Overcoming Java shortcomings
One believes that the verbosity and the statically typed nature of Java make it hardly suitable for expressing builds.
Jerkar tends to prove the opposite :
* Jerkar transparently compiles the java build classes prior to execute them. This step is very quick, Jerkar velocity does not suffer from this 'extra' step
* Jerkar heavily relies on convention and sensitive defaults : you only need to specify what is not 'standard'
* Jerkar features fluent APIs whose allow to express tasks in a very concise way. Jerkar build classes are close to Gradle script concision (and even more in certain cases) 
* Jerkar comes with no 3rd party dependency (except Ivy) to avoid version clashing.
* Jerkar keeps the runtime simple (no bytecode enhancement) for easier debugging 

# Main features
Jerkar provides what a self respecting modern, enterprise scale, build system should and more :
* Provides both APIs and a command line tool.
* Multi level of configuration system (Jerkar instance, user, build class, build command line)
* Powerfull dependency management (back-ended by Ivy so compatible with Maven repositories)
* Publication on Ivy or Maven repositories
* Multi-project support
* Powerfull fluent API to manipulate files, perform  compilations, tests, archives and all build related stuff
* Choice between free form builds (ala Ant) and enforced build templates (ala Maven)
* Hierarchical log output tracking execution time for each intermediate step
* Pluggable architecture
* Ability to get information from naming convention and Eclipse files, so in simpler cases you won't need to write script at all (even to generate war or perform SonarQube analysis) !!!


The documentation is at its very early stage but the code is yet pretty close to completion for a first release. 
I mainly need help for further testing, writing documentation, polishing the API... and getting some feedback of course.

# How to build
Jerkar is made of following projects :
* core : complete Jerkar project but without embedding following plugins
* plugins-jacoco : a plugin to perform test coverage
* plugins-sonar : a plugin to perform sonar analysis
* distrib-all : the core distrib augmented with the above plugins

Jerkar builds with itself. To get Jerkar full distrib built from the Java sources only, the simpler is to import these 4 projects in Eclipse, then :
* Create a Java Application run configuration (Run Configuration ... -> Java Application -> new)
  * Make sure that the Runtime JRE is a JDK (6 or above)
  * Choose `org.jerkar.distrib-all` as project
  * Choose `org.jerkar.Main` as Main class
* Run it : It will launch a multi-project build. You will find result for the full distrib in *org.jerkar.distrib-all/build/output* directory 

# Quick start
1. Add the org.jerkar.core-fat.jar (found in the distrib) in your IDE build-path. This jar includes Jerkar core along plugins classes.
2. Create a `build/spec` folder at the base of your project and make it a source folder in your IDE. In Jerkar, all related build stuff (build definition, local 3rd party libs, produced artifacts,...) lies under *build* directory.
3. Write the build class extending JkJavaBuild in this directory (in whatever package).
4. If your project respect convention, do not need managed dependencies and don't do 'special' thing, you don't even need 2) and 3) points.
5. Launch the `org.jerkar.Main` class in your IDE or type `jerkar` in the command line (with the root of your project as working directory).

This will launch the `doDefault` method defined in your build class. Note that this method is declared in the `JkJavaBuild` and invoke in sequence clean, compile, unitTest and pack methods.

If you want to launch several methods of your build, type `jerkar doSomething doSomethingElse`. Jerkar recognizes any public zero-argument method returning `void` as build method.
Type `jerkar help` to get all the build methods provided by your build class. 
  

## Example : Let's see how Jerkar core build itself

Jerkar core build is not complex but do some specific stuff : apart making standard compilation, junit tests and jar packaging, it constructs a distribution archive gathering jars, sources, property, readme files and executable.
This is the build class :

```java
    public class CoreBuild extends JkJavaBuild {

	    public File distripZipFile; // The zip file that will contain the whole distrib

	    public File distribFolder;  // The folder that will contain the whole distrib

	    @Override
	    protected void init() {
	        distripZipFile = ouputDir("jerkar-distrib.zip");
            distribFolder = ouputDir("jerkar-distrib");
		    this.fatJar = true;
        }

	    // Interpolize resource files replacing ${version} by a timestamp (in the Manifest)
	    @Override
	    protected JkResourceProcessor resourceProcessor() {
		    return super.resourceProcessor().with("version", version().name() + " - built at - " + buildTimestamp());
	    }

	    // Include the making of the distribution into the application packaging.
	    @Override
	    public void pack() {
		    super.pack();
		    distrib();
	    }

        // Create a distribution of Jerkar core, including jars, sources and windows/linux launch scripts
	    private void distrib() {
		    final JkDir distrib = JkDir.of(distribFolder);
		    JkLog.startln("Creating distrib " + distripZipFile.getPath());
		    final JkJavaPacker packer = packer();
		    distrib.importDirContent(baseDir("src/main/dist"));
		    distrib.importFiles(packer.jarFile(), packer.fatJarFile());
		    distrib.sub("libs/required").importDirContent(baseDir("build/libs/compile"));
		    distrib.sub("libs/sources").importDirContent(baseDir("build/libs-sources"))
		                                  .importFiles(packer.jarSourceFile());
			distrib.zip().to(distripZipFile, Deflater.BEST_COMPRESSION);
			JkLog.done();
	    }
	}
```

Notice that we need only to specify what is not 'standard'
* group and project name are inferred from the project folder name ('org.jerkar.core' so group is 'org.jerkar' and project is 'core')
* version is not specified, so by default it is `1.0-SNAPSHOT`(unless you inject the version via the command line using `-forcedVersion=Xxxxx`)
* sources, resources and tests folder are located on the conventional folders (same as Maven).
* this build class relies on local dependencies (dependencies located conventionally inside the project) so we don't need to mention them

A dependency managed flavor of this build is [CoreDepManagedBuild.java](https://github.com/jerkar/jerkar/blob/master/org.jerkar.core/build/spec/org/jerkar/CoreDepManagedBuild.java)

To launch the build for creating distrib from the command line, simply type : 

    jerkar

This will interpole resources (replacing ${version} by a timestamp everywhere), compile, run unit tests, create jars and package the distrib in zip file. 
This command is equivalent to `jerkar doDefault` : when no method specified, Jerkar invoke the `doDefault` method. Build result is *output* folder : 

![image of project layout](https://github.com/jerkar/jerkar/blob/master/doc/project-layout.png)

---
To launch a SonarQube analysis along test coverage and producing javadoc: 

    jerkar clean compile unitTest sonar#verify javadoc jacoco# -verbose=true
    
This will compile, unit test with test coverage, launch a sonar analysis with sonar user settings and finally produce the javadoc. 
- `clean`, `compile`, `unitTest` and `javadoc` are build methods inherited by `CoreBuild`
- `sonar#verify` means that Jerkar will invoke a method called `verify`in the `sonar` plugin class
- `jacoco#` means that the `jacoco` plugin will be activated while the junit test will be running
- `-verbose=true`means that the log will display verbose information ('-' prefix is the way to pass parameter in Jerkar)


Notice that Jacoco test coverage and SonarQube analysis are triggered without mention in the build class ! 
    
    
        
## Jerkar Runtime
----

This section details what happens behind the cover when Jerkar is run.

### Launching Java Process
 
Jerkar is a pure Java application requiring __JDK 6 or above__. __JDK__ is required and __JRE__ is not sufficient.
Indeed Jerkar uses the JDK tools to compile build definition files.

To ease launching Java process in command line, Jerkar provides native scripts ( _jerkar.bat_ for __Windows__ and _jerkar_ for __Unix__ ).
These scripts do the following :

1. __Find the java executable path__ : If a `JAVA_HOME` environment variable is defined then it takes its value as `java` path. Otherwise it takes the `java` executable defined in the _PATH_ of your OS.
2. __Get java execution option__ : If an environment variable `JERKAR_OPTS` exists then its value is passed to the `java` command line parameters, otherwise default `-Xmx512m -XX:MaxPermSize=512m` is passed.
3. __Set Jerkar classpath__ in the following order :
	* all jar and zip files found under _[WORKING DIR]/build/libs/build_
	* all jar and zip files found under _[JERKAR HOME]/libs/ext_
	* the _[JERKAR_HOME]/org.jerkar.core.jar_ file 
4. __Run the `org.jerkar.tool.Main` class__ passing the command line argument as is. So if you have typed `jerkar myArg1 myArg2` the `myArg1 myArg2` will be passed as Java command-line arguments.

#### Embedded Mode
Note that ___[JERKAR_HOME]/org.jerkar.core.jar___ comes after ___[WORKING_DIR]/build/libs/build/*___ in the classpath.
This means that if a version of Jerkar (org.jerkar.core.jar) is in this directory, the build will be processed with this instance of Jerkar and not with the one located in in _[JERKAR HOME]_.

This is called the __Embedded__ mode. It guarantees that your project will build regardless of Jerkar version installed on the host machine. 
This mode allows to build your project even if Jerkar is not installed on the host machine. just execute `java -cp build/libs/build/* org.jerkar.tool.Main` instead of `jerkar`.

### Jerkar Execution

The `org.jerkar.tool.Main#main` is the entry point of Jerkar. This is the method you invoke to launch or debug a Jerkar build within your IDE.

It processes as follow :

1. Parse the command line.
2. Populate system properties and Jerkar options from configuration files and command line (see <strong>build configuration</strong>).
3. Pre-process and compile build definition files (see <strong>Build Definition Compilation</strong>). 
4. Instantiate the build class (see <strong>Build Class Instantiation</strong>)
5. Inject options in build instance fields along the project root directory (see <strong>Build Configuration</strong>).
6. Call the `init()` method on the build instance. This is the place to set location related variable and to configure plugins. 
7. Instantiate and configure plugins specified in command line arguments.
8. Invoke methods specified in command line arguments : methods are executed in the order they appear on the command line.

#### Build Definition File Compilation
Jerkar compiles the build definition files prior to execute it. The build definition files are expected to be in _[PROJECT DIR]/build/def_. If this directory does not exist or does not contains java sources, the compilation is skipped.
Compilation outputs class files in _[PROJECT DIR]/build/output/def-bin_ directory and uses classpath containing :

* Java libraries located in _[PROJECT DIR]/build/libs/build_.
* Java libraries located in _[JERKAR HOME]/libs/ext_ (not in embedded mode).

You can augment the classpath with :

* Java libraries hosted on a Maven or Ivy repositories
* Java libraries located on file system.
* Build definition (java sources) of other projects

Information about extra lib to add to classpath are located in the build definition files, inside `@JkImport` and `@JkProject` annotation.
To read this information, build definition files are parsed prior to be compiled in order to extract classpath from source code.

##### Libraries Located on Maven/Ivy Repository 
To add libraries from Maven/Ivy repository you need to annotate the build definition with `@JkImport`. This annotation takes an array of String as its default parameter so you can specify several dependencies.
The mentioned dependencies are resolved transitively. 

``` 
@JkImport(`{"commons-httpclient:commons-httpclient:3.1", "com.google.guava:guava:18.0"})
public class HttpClientTaskBuild extends JkJavaBuild {`
...
```

Url of the maven/ivy repository is given by `downloadRepoUrl` Jerkar option (or it uses Maven Central if this option is not specified).
If this repository needs credentials, you need to supply it through Jerkar options `dowloadRepoUsername` and `downloadRepoPassword`.
 
If the download repository is an Ivy repo, you have to prefix url with `ivy:` so for example you'll get `ivy:http://my.ivy/repo`.

##### Libraries on File System
To add library from file system you need to annotate the build definition with `@JkImport`. This annotation takes an array of String as argument so you can specify several dependencies.
The mentioned dependencies are not resolved transitively. 
The expected value is a Ant include pattern applied to the project root directory.


``` 
@JkImport(`{"commons-httpclient:commons-httpclient:3.1", "build/libs/compile/*.jar"})
public class HttpClientTaskBuild extends JkJavaBuild {`
...
```

This will include _commons-httpclient_ and its dependencies in the classpath along all jar file located in _[PROJECT DIR]/build/libs/compile_.

##### Build Definitions of Other Project
Your build definitions can depends on build definitions of other projects. It is typically the case for multi-project builds. 
This capability allows to share build elements in a static typed way as the build definitions files can consume classes coming from build definitions of other projects.

`@JkProject` is an annotation that applies on fields instance of `org.jerkar.tool.JkBuild` or its subclasses. This annotation contains the relative path of the consumed project.
If the project build definition sources contain some `@JkProject` annotations, the build definition files of the consumed project are pre-processed and compiled recursively. 
The classes and the classpath of the consumed  project are added to the build definition classpath of the consumer project.

```
public class DistribAllBuild extends JkBuildDependencySupport {
	
	@JkProject("../org.jerkar.plugins-sonar")
	PluginsSonarBuild pluginsSonar;
	
	@JkProject("../org.jerkar.plugins-jacoco")
	PluginsJacocoBuild pluginsJacoco;
	
	@JkDoc("Construct a distrib assuming all dependent sub projects are already built.")
	public void distrib() {
		
		JkLog.startln("Creating distribution file");
		
		JkLog.info("Copy core distribution localy.");
		CoreBuild core = pluginsJacoco.core;  // The core project is got by transitivity
		File distDir = this.ouputDir("dist");
		JkFileTree dist = JkFileTree.of(distDir).importDirContent(core.distribFolder);
		...
```

#### Build Class Instantiation
Once the build definition files compiled. Jerkar instantiate the build class.
The build class is specified by the `buildClass` option if present. If not, it is the first class implementing `org.jerkar.tool.JkBuild`. 
If no class implementing `org.jerkar.tool.JkBuild` is found then the `org.jerkar.tool.builtins.javabuild.JkJavaBuild` is instantiated.

The class scanning processes classes in alphabetic order then subpackage in deep first. This mean that class `MyBuid` will be scanned prior `apackage.ABuild`, and `aa.bb.MyClass` will be scanned prior `ab.OtherClass`.

The `buildClass` option can mention a simple name class (class name omitting its package). If no class matches the  specified `buildClass` then an exception is thrown.

The `org.jerkar.tool.JkBuild` constructor instantiate fields annotated with `@JkProject`. If a project build appears many time in the annotated project tree, a single instance is created then shared.


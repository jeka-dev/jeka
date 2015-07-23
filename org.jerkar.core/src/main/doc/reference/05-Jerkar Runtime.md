## Jerkar runtime
----

This section details what happens behind the cover when Jerkar is run.

### Launching Java Process
 
Jerkar is a pure Java application requiring __JDK 6 or above__. __JDK__ is required and __JRE__ is not sufficient.
Indeed Jerkar uses the JDK tools for compiling build definitions and generate Javadoc.

For an easy launching of the java process in command line, Jerkar provides native scripts ( _jerkar.bat_ for __Windows__ and _jerkar_ for __Unix__ ).
These scripts do the following :

1. __Find the java executable path__ : If a `JAVA_HOME` environment variable is defined then it takes this value as `java` path. Otherwise it takes the `java` executable defined in the _PATH_ environment variable if any.
2. __Get java execution option__ : If an environment variable `JERKAR_OPTS` exists then its value will be passed in the `java`command line parameters, otherwise default `-Xmx512m -XX:MaxPermSize=512m` is passed.
3. __Set Jerkar classpath__ in the following order :
	* all jar and zip files found under _[WORKING DIR]/build/libs/build_
	* all jar and zip files found under _[JERKAR HOME]/libs/ext_
	* the _[JERKAR_HOME]/org.jerkar.core.jar_ file 
4. __Run the `org.jerkar.tool.Main` class__ passing the command line argument as is. So if you have typed `jerkar myArg1 myArg2` the `myArg1 myArg2` will be passed as Java command-line arguments.

#### Embedded mode
Note that ___[JERKAR_HOME]/org.jerkar.core.jar___ comes after ___[WORKING_DIR]/build/libs/build/*___ in the classpath.
This means that if a version of Jerkar (org.jerkar.core-fat.jar) is in this directory, then the build will be processed with this instance of Jerkar and not with the one located in in _[JERKAR HOME]_.

This is called the __Embedded__ mode. It guarantees that your project will build regardless of the Jerkar version installed on the host machine. 
This mode allows to build your project even if Jerkar is not installed on the host machine. just execute `java -cp build/libs/build/* org.jerkar.tool.Main` instead of `jerkar`.

### Jerkar execution

The `org.jerkar.tool.Main#main` is the entry point of Jerkar. It's the method you invoke to launch/debug a Jerkar build within your IDE.

It processes as follow :

1. Parse the command line.
2. Populate the system properties and Jerkar options from configuration files and command line (see <strong>build configuration</strong>).
3. Pre-process Java source files located under _[WORKING DIR]/build/def_ directory if any. The preprocessor will parse source code to extract content of `@JkImport` and `@JkProject` Java annotations. 
The `@JkImport` contains the binary dependencies on modules hosted in Maven/Ivy repository while the `@JkProject` contains dependency on build definition classes of an other project.
If the project build definition sources contain some `@JkProject` annotations, the dependee project are pre-processed and compiled recursively prior to go next. 
4. Compile Java source files located under _[WORKING DIR]/build/def_ directory. The compilation is done using classpath constituted in the prevous step.
5. Instantiate the build class. The build class is the class specified by the `buildClass` option if present. If not, it is the first class implementing `org.jerkar.tool.JkBuild`. 
The class scanning processes classes in alphabetic order then subpackage in deep first. This mean that class `MyBuid` will be scanned before `apackage.ABuild`.
If no class implementing `org.jerkar.tool.JkBuild` is found then the `org.jerkar.tool.builtins.javabuild.JkJavaBuild` is instantiated.
The `buildClass` option can mention a simple name class (class name omitting its package).
If no class matches the  specified `buildClass` then an exception is thrown.
6. Inject options in build instance fields  (see <strong>build configuration</strong>).
7. Call the `init()` method on the build instance.
8. Instantiate and bind plugins.
9. Invoke methods specified in arguments.

 

<br/>

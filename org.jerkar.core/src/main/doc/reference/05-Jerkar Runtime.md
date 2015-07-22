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
Note that ___[JERKAR_HOME]/org.jerkar.core.jar___ comes after ___[WORKING DIR]/build/libs/build/*___ in the classpath.
This means that if a version of Jerkar (org.jerkar.core-fat.jar) is in this directory, then the build will be processed with this instance of Jerkar and not with the one located in in _[JERKAR HOME]_.

This is called the __Embedded__ mode. It guarantees that your project will build regardless of the Jerkar version installed on the host machine. 
This mode allows to build your project even if Jerkar is not installed on the host machine. just execute `java -cp build/libs/build/* org.jerkar.tool.Main` instead of `jerkar`.

<br/>

# Getting Started
------------------

## Install Jerkar

1. unzip the [distribution archive](http://jerkar.github.io/binaries/jerkar-distrib.zip) to the directory you want to install Jerkar : let's call it _[Jerkar Home]_
2. make sure that either a valid *JDK* (6 or above) is on your _PATH_ environment variable or that a _JAVA_HOME_ variable is pointing on
3. add _[Jerkar Home]_ to your _PATH_ environment variable
4. execute `jerkar` in the command line. You should get an output starting by : 

<pre><code>
 _______           _
(_______)         | |
     _ _____  ____| |  _ _____  ____
 _  | | ___ |/ ___) |_/ |____ |/ ___)
| |_| | ____| |   |  _ (/ ___ | |
 \___/|_____)_|   |_| \_)_____|_|
                                     The 100% Java build tool.
Java Home : C:\UserTemp\I19451\software\jdk1.6.0_24\jre
Java Version : 1.6.0_24, Sun Microsystems Inc.
Jerkar Home : C:\software\jerkar                             
Jerkar User Home : C:\users\djeang\.jerkar
</code></pre>


The default download repository (for fetching dependencies) is set to maven central (http://repo1.maven.org/maven2) by default. If you want to use another default, edit _[Jerkar Home]/options.properties_ and add the following property `repo.download.url=http://my.personal/repo`.
The Jerkar Home is the one given by the `Jerkar` output as mentioned above (C:\users\djeang\.jerkar).  

## Use Jerkar with command line

Let's see first how we can use Jerkar using command line only.

### Create a project
1. Create a new folder as root of your new project.
2. Execute `jerkar scaffold` under the project base directory. This generates the project structures.

It's done.

### Build the project
1. Edit the Build.java source files under [project root]/build/def folder if needed.
2. Execute `jerkar` under the project base directory. 

It's done. For more about using command line, please visit [Tour](../../tour.html). 


## Use Jerkar with Eclipse (without Jerkar [plugin] (https://github.com/jerkar/eclipsePlugin4Jerkar) )

### Setup Eclipse 

To use Jerkar within Eclipse, you just have to set 2 variables in Order Eclipse find Jerkar binaries and your local repository hosting dependencies.

1. Open the Eclipse preference window : _Window -> Preferences_
2. Navigate to the classpath variable panel : _Java -> Build Path -> Classpath Variables_
3. Add these 2 variables :
    * `JERKAR_HOME` which point to _[Jerkar Home]_, 
    * `JERKAR_REPO` which point to _[Jerkar User Home]_/.jerkar/cache/repo_.
    
Note : By default _[Jerkar User Home]_ point to _[User Home]_ but can be overrided by defining the environment variable `JERKAR_USER_HOME`. 
  
  
If you have any problem to figure out the last value, just execute `jerkar` from anywhere and it will start logging the relevant information :

```
 _______    	   _
(_______)         | |                
     _ _____  ____| |  _ _____  ____ 
 _  | | ___ |/ ___) |_/ |____ |/ ___)
| |_| | ____| |   |  _ (/ ___ | |    
 \___/|_____)_|   |_| \_)_____|_|
                                     The 100% Java build tool.

Java Home : C:\UserTemp\I19451\software\jdk1.6.0_24\jre
Java Version : 1.6.0_24, Sun Microsystems Inc.
Jerkar Home : C:\software\jerkar                               <-- This is the value for JERKAR_HOME
Jerkar User Home : C:\users\djeang\.jerkar
Jerkar Repository Cache : C:\users\djeang\.jerkar\cache\repo   <-- This is the value for JERKAR_REPO
...
```

### Configure a Jerkar project

To use Jerkar inside an Eclipse project, you have to add Jerkar lib, build/def folder and project dependencies to the build path.
Jerkar can do it for you be executing `jerkar eclipse#generateFiles` from project root folder.

### Launch/Debug from Eclipse

Just launch the `org.jerkar.tool.Main` class in your IDE with the root of your project as working directory. 
You can also write a `main` method in build class and execute it. 

```
public static void main(String[] args) {
		
	 // This main method will run the 'doDefault' method of 'MyBuild' class. 		
    JkInit.instanceOf(MyBuild.class, args).doDefault(); 
}

```

## Migrate projects from Maven to Jerkar

Prerequisites are :

* Maven is installed on the machine running Jerkar.
* The project builds with Maven (at least, `mvn help:effective-pom` should succeed).

Steps :

1. Execute `jerkar scaffold` at the root folder of the project you want to migrate  (Let's call it [PROJECT DIR]). It generates a build class at _[PROJECT DIR]/buid/def/Build.java_
2. If you are an eclipse user, execute `jerkar eclipse#generateFiles`. It generates _.project_ and _.classpath_ so the project is now usable in Eclipse.

## Play with 'samples' project

<p class="alert alert-warning">
	<strong>Prerequisite:</strong> You need to have Git installed on your IDE to download sample projects.
</p>

To provide an easy hand on experience, you can play with **org.jerkar.samples** project hosted in [Jerkar Git Repository](https://github.com/jerkar/jerkar.git).

This project contain a very minimalist java application (ala HelloWorld) and several build classes to build the project. All these classes are located under *build/def* directory. These build classes are independent from each other, each one illustrates one way of doing thing in Jerkar. Some of these build classes declare dependencies that are not even needed for the project but for illustration purpose only.

To play with _samples_ project, you should :

1. import both **org.jerkar.samples** project in your IDE
   -> Normally the IDE displays error marks cause the build path is not properly set up.

2. Eclipse users : execute <code>jerkar eclipse#generateFiles</code> at the root of the project
   -> This generates Eclipse .classpath file. 
   If you refresh your workspace, these two projects should build in Eclipse without error marks.
   The project contains Eclipse launchers, so you might found launcher that execute the above command line in _External tools_ entries.
   
3. To launch a Jerkar using on a particular build class, mention the class name as  `jerkar -buildClass=MyBuildClassName clean compile` for example. Note that you don't have to specified the full qualified class name, the simple name is enough. 

4. Edit code and modify/run some build class to play with Jerkar.    

### Playing with `AClassicalBuild`

By default, when no `-buildClass=Xxxx` is specified, Jerkar takes the first one in alphabetical order. Let's take this class for the following steps.

#### Getting some help

You can have an exhaustive list of invokable method and possible options for a build by executing `jerkar help` at the root of the project. This leads in such output :

```
...

Help on build class org.jerkar.samples.AClassicBuild
----------------------------------------------------

----------------------
Methods               
----------------------

From org.jerkar.tool.JkBuild
----------------------------
clean : Clean the output directory.

help : Display all available methods defined in this build.

helpPlugins : Display details on all available plugins.

scaffold : Create the project structure

verify : Run checks to verify the package is valid and meets quality criteria.

From org.jerkar.tool.builtins.javabuild.JkJavaBuild
---------------------------------------------------
compile : Generate sources and resources, compile production sources and process production resources to the classes directory.

doCompile : Lifecycle method :#compile. As doCompile is the first stage, this is equals to #compile

doDefault : Method executed by default when none is specified. By default this method equals to #clean + #doPack

doPack : Lifecycle method : #doUnitTest + #pack

doPublish : Lifecycle method : #doVerify + #publish

doUnitTest : Lifecycle method : #doCompile + #unitTest

doVerify : Lifecycle method : #doUnitTest + #pack

javadoc : Produce documents for this project (javadoc, Html site, ...)

pack : 
Create many jar files containing respectively binaries, sources, test binaries and test sources.
The jar containing the binary is the one that will be used as a depe,dence for other project.

publish : 
Publish the produced artifact to the defined repositories. 
This can work only if a 'publishable' repository has been defined and the artifact has been generated (pack method).

unitTest : Compile and run all unit tests.

----------------------
Options               
----------------------

From org.jerkar.samples.AClassicBuild
-------------------------------------
extraBuildPath : 
Mention if you want to add extra lib in your build path.
It can be absolute or relative to the project base dir.
These libs will be added to the build path to compile and run Jerkar build class.
Example : -extraBuildPath=C:\libs\mylib.jar;libs/others/**/*.jar
Type : String
Default value : null

extraPath.compile : 
compile scope : these libs will be added to the compile and runtime path.
Example : -extraPath.compile=C:\libs\mylib.jar;libs/others/**/*.jar
Type : String
Default value : null

extraPath.provided : 
provided scope : these libs will be added to the compile path but won't be embedded in war files or fat jars.
Example : -extraPath.provided=C:\libs\mylib.jar;libs/others/**/*.jar
Type : String
Default value : null

extraPath.runtime : 
runtime scope : these libs will be added to the runtime path.
Example : -extraPath.runtime=C:\libs\mylib.jar;libs/others/**/*.jar
Type : String
Default value : null

extraPath.test : 
test scope : these libs will be added to the compile and runtime path.
Example : -extraPath.test=C:\libs\mylib.jar;libs/others/**/*.jar
Type : String
Default value : null

pack.checksums : Comma separated list of algorithm to use to produce checksums (ex : 'sha-1,md5').
Type : String
Default value : md5

pack.fatJar : When true, produce a fat-jar, meaning a jar embedding all the dependencies.
Type : boolean
Default value : false

pack.signWithPgp : When true, the produced artifacts are signed with PGP.
Type : boolean
Default value : false

pack.tests : When true, tests classes and sources are packed in jars.
Type : boolean
Default value : false

repo.download.password : Password to connect to the repository (if needed).
Type : String
Default value : null

repo.download.url : Url of the repository : Prefix the Url with 'ivy:' if it is an Ivy repostory.
Type : String
Default value : http://i-net1102e-prod:8081/nexus/content/groups/bnppf-secured

repo.download.username : 
Usename to connect to repository (if needed).
Null or blank means that the repository will be accessed in an anonymous way.
Type : String
Default value : null

repo.publish.password : Password to connect to the repository (if needed).
Type : String
Default value : null

repo.publish.url : Url of the repository : Prefix the Url with 'ivy:' if it is an Ivy repostory.
Type : String
Default value : file:///C:/usertemp/i19451/jerkar-maven-repo

repo.publish.username : 
Usename to connect to repository (if needed).
Null or blank means that the repository will be accessed in an anonymous way.
Type : String
Default value : null

repo.release.password : Password to connect to the repository (if needed).
Type : String
Default value : null

repo.release.url : Url of the repository : Prefix the Url with 'ivy:' if it is an Ivy repostory.
Type : String
Default value : https://oss.sonatype.org/content/repositories/snapshots/

repo.release.username : 
Usename to connect to repository (if needed).
Null or blank means that the repository will be accessed in an anonymous way.
Type : String
Default value : null

tests.fork : Turn it on to run tests in a forked process.
Type : boolean
Default value : false

tests.jvmOptions : Argument passed to the JVM if tests are forked. Example : -Xms2G -Xmx2G
Type : String
Default value : null

tests.report : 
The more details the longer tests take to be processed.
BASIC mention the total time elapsed along detail on failed tests.
FULL detailed report displays additionally the time to run each tests.
Example : -report=NONE
Type : Enum of NONE, BASIC, FULL
Default value : BASIC

tests.skip : Turn it on to skip tests.
Type : boolean
Default value : false

version : Version to inject to this build. If 'null' or blank than the version will be the one returned by #version()
Type : String
Default value : 1.0.0

Type 'jerkar helpPlugins' to get help on plugins

``` 
You can also define on which build class you want help on : `jerkar help -buildClass=AntStyleBuild`. 


#### Compile, test and package the application.

Just execute `jerkar`. By default, it invoke the `doDefault` method. If you browse code, you'll figure out quite easily that this method actualy does a : `clean`, `compile`, `unitTest` and pack the application.

This should lead in the following log output :

```
 _______           _                 
(_______)         | |                
     _ _____  ____| |  _ _____  ____ 
 _  | | ___ |/ ___) |_/ |____ |/ ___)
| |_| | ____| |   |  _ (/ ___ | |    
 \___/|_____)_|   |_| \_)_____|_|
                                     The 100% Java build tool.

Working Directory : C:\UserTemp\I19451\git\jerkar\org.jerkar.samples
Java Home : C:\UserTemp\I19451\software\jdk1.6.0_24\jre
Java Version : 1.6.0_24, Sun Microsystems Inc.
Jerkar Home : C:\UserTemp\I19451\git\jerkar\org.jerkar.distrib-all\build\output\dist
Jerkar User Home : H:\AppData.W7\Sun\.jerkar
Jerkar Repository Cache : H:\AppData.W7\Sun\.jerkar\cache\repo
Jerkar Classpath : C:\UserTemp\I19451\git\jerkar\org.jerkar.distrib-all\build\output\dist\libs\ext\org.jerkar.plugins-jacoco.jar;C:\UserTemp\I19451\git\jerkar\org.jerkar.distrib-all\build\output\dist\libs\ext\org.jerkar.plugins-sonar.jar;C:\UserTemp\I19451\git\jerkar\org.jerkar.distrib-all\build\output\dist\org.jerkar.core.jar
Command Line : 
Specified System Properties : 
  http.nonProxyHosts=localhost|i-net*
  http.proxyHost=nwbcproxy.res.sys.shared.fortis
  http.proxyPort=8080
  https.nonProxyHosts=localhost|i-net*
  https.proxyHost=nwbcproxy.res.sys.shared.fortis
  https.proxyPort=8080
Standard Options : buildClass=null, verbose=false, silent=false
Options : 
  pgp.pubring=C:/UserTemp/I19451/git/jerkar/org.jerkar.core/src/test/java/org/jerkar/api/crypto/pgp/pubring.gpg
  pgp.secretKeyPassword=*****
  pgp.secring=C:/UserTemp/I19451/git/jerkar/org.jerkar.core/src/test/java/org/jerkar/api/crypto/pgp/secring.gpg
  repo.download.url=http://i-net1102e-prod:8081/nexus/content/groups/bnppf-secured
  repo.publish.url=file:///C:/usertemp/i19451/jerkar-maven-repo
  sonar.host.url=http://i-net1101e-prod:9000/
  sonar.jdbc.password=*****
  sonar.jdbc.url=jdbc:oracle:thin:@dbopcfso01p:48892:CFSO01P
  sonar.jdbc.username=cfso_usr

---------------------------------------------------
Making build classes for project org.jerkar.samples
---------------------------------------------------
|  Resolving dependencies for scope 'build' ... 
|  |  3 artifacts: [commons-codec:1.2, commons-logging:1.0.4, commons-httpclient:3.1]
|   \ Done in 0.279 seconds.
|  Compiling 7 source files using options : -d C:\UserTemp\I19451\git\jerkar\org.jerkar.samples\build\output\def-bin -cp C:\UserTemp\I19451\git\jerkar\org.jerkar.distrib-all\build\output\dist\libs\ext\org.jerkar.plugins-jacoco.jar;C:\UserTemp\I19451\git\jerkar\org.jerkar.distrib-all\build\output\dist\libs\ext\org.jerkar.plugins-sonar.jar;C:\UserTemp\I19451\git\jerkar\org.jerkar.distrib-all\build\output\dist\org.jerkar.core.jar;H:\AppData.W7\Sun\.jerkar\cache\repo\commons-httpclient\commons-httpclient\jars\commons-httpclient-3.1.jar;H:\AppData.W7\Sun\.jerkar\cache\repo\commons-logging\commons-logging\jars\commons-logging-1.0.4.jar;H:\AppData.W7\Sun\.jerkar\cache\repo\commons-codec\commons-codec\jars\commons-codec-1.2.jar ... 
|   \ Done in 0.375 seconds.
 \ Done in 1.005 seconds.

Setting build execution classpath to : C:\UserTemp\I19451\git\jerkar\org.jerkar.distrib-all\build\output\dist\libs\ext\org.jerkar.plugins-jacoco.jar;C:\UserTemp\I19451\git\jerkar\org.jerkar.distrib-all\build\output\dist\libs\ext\org.jerkar.plugins-sonar.jar;C:\UserTemp\I19451\git\jerkar\org.jerkar.distrib-all\build\output\dist\org.jerkar.core.jar;H:\AppData.W7\Sun\.jerkar\cache\repo\commons-httpclient\commons-httpclient\jars\commons-httpclient-3.1.jar;H:\AppData.W7\Sun\.jerkar\cache\repo\commons-logging\commons-logging\jars\commons-logging-1.0.4.jar;H:\AppData.W7\Sun\.jerkar\cache\repo\commons-codec\commons-codec\jars\commons-codec-1.2.jar;C:\UserTemp\I19451\git\jerkar\org.jerkar.samples\build\output\def-bin
----------------------------------------------
Executing build for project org.jerkar.samples
----------------------------------------------
Build class org.jerkar.samples.AClassicBuild
Activated plugins : []
Field values : 
  extraBuildPath=null
  extraPath.compile=null
  extraPath.provided=null
  extraPath.runtime=null
  extraPath.test=null
  pack.fatJar=false
  pack.signWithPgp=false
  pack.tests=false
  repo.download.password=*****
  repo.download.url=http://i-net1102e-prod:8081/nexus/content/groups/bnppf-secured
  repo.download.username=null
  repo.publish.password=*****
  repo.publish.url=file:///C:/usertemp/i19451/jerkar-maven-repo
  repo.publish.username=null
  repo.release.password=*****
  repo.release.url=https://oss.sonatype.org/content/repositories/snapshots/
  repo.release.username=null
  tests.fork=false
  tests.report=BASIC
  tests.skip=false
  version=1.0.0
Method : doDefault
------------------
Cleaning output directory C:\UserTemp\I19451\git\jerkar\org.jerkar.samples\build\output ...  \ Done in 0.011 seconds.
Processing production code and resources ... 
|  Setting dependency resolver  ... 
|   \ Done : Resolver set [JkModuleDependency=com.google.guava.guava:18.0[[compile]], JkModuleDependency=com.sun.jersey.jersey-server:1.19[[compile]], JkModuleDependency=com.orientechnologies.orientdb-client:2.0.8[[compile]], JkModuleDependency=junit:4.11[[test]], JkModuleDependency=org.mockito.mockito-all:1.9.5[[test]]] in 0.0060 seconds.
|  Resolving dependencies for scope 'compile' ... 
|  |  9 artifacts: [net.java.dev.jna.jna:4.0.0, com.google.guava.guava:18.0, com.sun.jersey.jersey-server:1.19, com.orientechnologies.orientdb-enterprise:2.0.8, net.java.dev.jna.jna-platform:4.0.0, com.googlecode.concurrentlinkedhashmap.concurrentlinkedhashmap-lru:1.4.1, com.orientechnologies.orientdb-client:2.0.8, org.xerial.snappy.snappy-java:1.1.0.1, com.orientechnologies.orientdb-core:2.0.8]
|   \ Done in 0.312 seconds.
|  Resolving dependencies for scope 'provided' ... 
|  |  0 artifacts: []
|   \ Done in 0.017 seconds.
|  Compiling 2 source files using options : -d C:\UserTemp\I19451\git\jerkar\org.jerkar.samples\build\output\classes -cp H:\AppData.W7\Sun\.jerkar\cache\repo\com.google.guava\guava\bundles\guava-18.0.jar;H:\AppData.W7\Sun\.jerkar\cache\repo\com.sun.jersey\jersey-server\jars\jersey-server-1.19.jar;H:\AppData.W7\Sun\.jerkar\cache\repo\com.orientechnologies\orientdb-client\jars\orientdb-client-2.0.8.jar;H:\AppData.W7\Sun\.jerkar\cache\repo\com.orientechnologies\orientdb-enterprise\jars\orientdb-enterprise-2.0.8.jar;H:\AppData.W7\Sun\.jerkar\cache\repo\com.orientechnologies\orientdb-core\jars\orientdb-core-2.0.8.jar;H:\AppData.W7\Sun\.jerkar\cache\repo\org.xerial.snappy\snappy-java\bundles\snappy-java-1.1.0.1.jar;H:\AppData.W7\Sun\.jerkar\cache\repo\com.googlecode.concurrentlinkedhashmap\concurrentlinkedhashmap-lru\jars\concurrentlinkedhashmap-lru-1.4.1.jar;H:\AppData.W7\Sun\.jerkar\cache\repo\net.java.dev.jna\jna\jars\jna-4.0.0.jar;H:\AppData.W7\Sun\.jerkar\cache\repo\net.java.dev.jna\jna-platform\jars\jna-platform-4.0.0.jar -source 1.6 -target 1.6 ... 
|   \ Done in 0.216 seconds.
|  Coping resource files to C:\UserTemp\I19451\git\jerkar\org.jerkar.samples\build\output\classes ... 
|   \ Done : 0 file(s) copied. in 0.0010 seconds.
 \ Done in 0.565 seconds.
Process unit tests ... 
|  Resolving dependencies for scope 'test' ... 
|  |  12 artifacts: [net.java.dev.jna.jna:4.0.0, com.google.guava.guava:18.0, com.sun.jersey.jersey-server:1.19, com.orientechnologies.orientdb-enterprise:2.0.8, net.java.dev.jna.jna-platform:4.0.0, com.googlecode.concurrentlinkedhashmap.concurrentlinkedhashmap-lru:1.4.1, com.orientechnologies.orientdb-client:2.0.8, org.xerial.snappy.snappy-java:1.1.0.1, com.orientechnologies.orientdb-core:2.0.8, org.mockito.mockito-all:1.9.5, junit:4.11, org.hamcrest.hamcrest-core:1.3]
|   \ Done in 0.213 seconds.
|  Compiling 1 source files using options : -d C:\UserTemp\I19451\git\jerkar\org.jerkar.samples\build\output\testClasses -cp C:\UserTemp\I19451\git\jerkar\org.jerkar.samples\build\output\classes;H:\AppData.W7\Sun\.jerkar\cache\repo\com.google.guava\guava\bundles\guava-18.0.jar;H:\AppData.W7\Sun\.jerkar\cache\repo\com.sun.jersey\jersey-server\jars\jersey-server-1.19.jar;H:\AppData.W7\Sun\.jerkar\cache\repo\com.orientechnologies\orientdb-client\jars\orientdb-client-2.0.8.jar;H:\AppData.W7\Sun\.jerkar\cache\repo\com.orientechnologies\orientdb-enterprise\jars\orientdb-enterprise-2.0.8.jar;H:\AppData.W7\Sun\.jerkar\cache\repo\com.orientechnologies\orientdb-core\jars\orientdb-core-2.0.8.jar;H:\AppData.W7\Sun\.jerkar\cache\repo\org.xerial.snappy\snappy-java\bundles\snappy-java-1.1.0.1.jar;H:\AppData.W7\Sun\.jerkar\cache\repo\com.googlecode.concurrentlinkedhashmap\concurrentlinkedhashmap-lru\jars\concurrentlinkedhashmap-lru-1.4.1.jar;H:\AppData.W7\Sun\.jerkar\cache\repo\net.java.dev.jna\jna\jars\jna-4.0.0.jar;H:\AppData.W7\Sun\.jerkar\cache\repo\net.java.dev.jna\jna-platform\jars\jna-platform-4.0.0.jar;H:\AppData.W7\Sun\.jerkar\cache\repo\junit\junit\jars\junit-4.11.jar;H:\AppData.W7\Sun\.jerkar\cache\repo\org.hamcrest\hamcrest-core\jars\hamcrest-core-1.3.jar;H:\AppData.W7\Sun\.jerkar\cache\repo\org.mockito\mockito-all\jars\mockito-all-1.9.5.jar -source 1.6 -target 1.6 ... 
|   \ Done in 0.102 seconds.
|  Coping resource files to C:\UserTemp\I19451\git\jerkar\org.jerkar.samples\build\output\test-classes ...
|   \ Done : 0 file(s) copied. in 0.0010 seconds.
|  Run JUnit tests ... 
|  |  1 test(s) run, 0 failure(s), 0 ignored. In 59 milliseconds.
|   \ Done : Tests run in 0.094 seconds.
 \ Done in 0.488 seconds.
Packaging module ... 
|  Creating zip file : C:\UserTemp\I19451\git\jerkar\org.jerkar.samples\build\output\org.jerkar.samples.jar ...  \ Done in 0.0060 seconds.
|  Creating zip file : C:\UserTemp\I19451\git\jerkar\org.jerkar.samples\build\output\org.jerkar.samples-sources.jar ...  \ Done in 0.0060 seconds.
 \ Done in 0.023 seconds.
Method doDefault success in 1.091 seconds.
  ______                                     _ 
 / _____)                                   | |
( (____  _   _  ____ ____ _____  ___  ___   | |
 \____ \| | | |/ ___) ___) ___ |/___)/___)  |_|
 _____) ) |_| ( (__( (___| ____|___ |___ |   _ 
(______/|____/ \____)____)_____|___/(___/   |_|
                                    
                                               Total build time : 2.225 seconds.
``` 
 
Have a look in the _build/output_ dir, you will see 2 jar files : the normal jar and the sources jar.

If you want the build create also tests jar, fat jar (jar containing all the dependencies) along checksums, execute `jerkar -pack.tests -pack.fatJar -pack.checksums=sha-1,md5`

Don't forget that you have many way to pass options (see <a href="reference.html">reference guide section 3.3</a>).<br/>

##### Passing options with property files

You can set it in *[project Dir]/build/def/build.properties* file, *[Jerkar User Dir]/options.jar* or *[Jerkar Home]/options.jar* files according how widely you want to spread the setup.

```
pack.tests=true
pack.fatJar=true
pack.checksums=md5
```

##### Harcoding options

You can also "hard code" it in the build definition class as :

```
public class AClassicBuild extends JkJavaBuild {

    {
      pack.tests=true;
      pack.fatJar=true;
      pack.checksums="md5"
    }
    ...
```

or

```
public class AClassicBuild extends JkJavaBuild {

    @Override
    protected void init() {
        pack.tests=true;
        pack.fatJar=true;
        pack.checksums="md5"
    }
    ...
```

or

```
public class AClassicBuild extends JkJavaBuild {

    public AClassicBuild() {
        pack.tests=true;
        pack.fatJar=true;
        pack.checksums="md5"
    }
    ...
```
  
 
#### Unactivating tests

If you want to skip tests, mention _tests.skip_ option such as `jerkar -tests.skip`.

#### Forking tests

If you want the test run in forked mode, mention _tests.fork_ as `jerkar -tests.fork` or `jerkar -tests.fork -tests.jvmOptions=-Xms16M -Xmx512`.

#### Generating verbose test report

Jerkar generates standard Junit test reports. You can make report more verbose by mentioning `tests.report=FULL`. Reports are generated in _build/output/test-reports/junit_ folder.

#### Producing a test coverage report

You just have to activate the Jacoco plugin as `jerkar jacoco#`. Reports are generated in _build/output/test-reports/jacoco/jacoco.exec_ file.

#### Triggering SonarQube analysis

You just have to activate the Jacoco plugin as `jerkar doVerify sonar#`. By default analysis is triggered on local SonarQube server (http://localhost:9000).
You can also execute `jerkar doVerify sonar# jacoco#`to trigger an analysis that include unit test coverage.
The sonar properties are already set according build information (source location, classpath, test location, encoding, ...) but you can set [any parameter](http://docs.sonarqube.org/display/SONAR/Analysis+Parameters) by defining it as option.

For example, you can add the following to your [Jerkar User Home]/options.properties

```
sonar.host.url=http://mySonarServer:8080
sonar.login=myLogin
sonar.password=myPassword
sonar.skipDesign=true
``` 

You can also "hard code" the settings in the build definition so you don't have to mention any options. See `SonarParametrizedBuild` class.

```
public class SonarParametrizedBuild extends JkJavaBuild {
	
	@Override
	protected void init() {
		JkBuildPluginSonar sonarPlugin = new JkBuildPluginSonar()
			.prop(JkSonar.HOST_URL, "http://mySonarServer:8080" )
			.prop(JkSonar.BRANCH, "myBranch");
		JkBuildPluginJacoco jacocoPlugin = new JkBuildPluginJacoco();
		this.plugins.activate(sonarPlugin, jacocoPlugin);
	}
...
```

With this definition, you only have to execute `jerkar doVerify` to trigger a SonarQube analysis including test coverage.
 
 
### Playing with the others build classes

If you want to play with other build classes available in the _samples_ project the same way you did for _AClassicalBuild_, just mention `-buildClass=Xxxxx` in the command line where `Xxxxx` stands for the short or complete name of the build class. So for example, `jerkar -buildClass=AntStyleBuild` is perfectly valid as `jerkar -buildClass=org.jerkar.samples.AntStyleBuild`. 

## Play with 'samples-dependee' project

This project illustrates how to handle multi-project builds. You can get it at [Jerkar Git Repository](https://github.com/jerkar/jerkar.git )..
This project depends on **org.jerkar.samples** project see previously..

This project contain two build classes :

* [`NormalJarBuild`](https://github.com/jerkar/jerkar/blob/master/org.jerkar.samples-dependee/build/def/NormalJarBuild.java) 
* [`FatJarBuild`](https://github.com/jerkar/jerkar/blob/master/org.jerkar.samples-dependee/build/def/FatJarBuild.java) 

Both build class code are fully commented to provide self explanation.


<br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/> 
<br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/> 
<br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/> 
<br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/> 





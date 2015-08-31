## Dependency Management
----

### What is a dependency ?

In this context, we call a __dependency__ an indication that can be resolved to a file (or a set of file) needed to accomplish certain part of the build.
So for example if a project _Foo_ has a __dependency__ _bar_, this means that _Foo_ may need the files indicated by _bar_ for building. 
In Jerkar code, the dependency concept is embodied by the abstract `JkDependency` class.

Jerkar distinguishes 3 types of __dependency__ :

* __Arbitrary files__ located on the file system (Embodied by `JkFileSystemDependency` class). These files are assumed to be present on the file system when the build is running.
* __Files produced by a computation__ (Embodied by `JkComputedDependency` class). These files may be present on file system or not. If they are not present, the computation is run in order to produce the missing files. Generally the computation stands for the build of an external project.
* __Reference to module__ (Embodied by `JkModuleDependency`) hosted in a binary repository (Ivy or Maven for instance) : Jerkar can consume and resolve transitively any artifact located in a repository as you would do with Maven or Ivy.

<p class="alert alert-success">
For the last, Jerkar is using <b>Ivy 2.4.0</b> under the hood. The library is embedded in the Jerkar jar and is executed in a dedicated classloader. So all happens as if there where no dependency on Ivy.
</p>

### What is a scope ?

Projects may need dependencies to accomplish certain tasks and needed dependencies may vary according the executed tasks.
For example, to __compile__ you may need _guava_ library only but to __test__ you'll need _junit_ library as well. 
To label dependencies according their usage, Jerkar uses the notion of __scope__ (embodied by `JkScope` class). This notion is similar to the Maven scope.

Scopes can __inherit__ from each other. This means that if a scope _Foo_ inherits from scope _Bar_ then a dependencies declared with scope _Bar_ will be also considered as declared with scope _Foo_.
For instance, in `JkJavaBuild`, scope `TEST` inherits from `RUNTIME` that inherits from `COMPILE` so every dependencies declared with scope `COMPILE` are considered to be declared with scope `RUNTIME` and `TEST` as well.   

By default, scopes are __transitive__. This has only a meaning for __reference to module__. 
If we have 3 modules having the following dependency scheme : `A` -> `B` -> `C` and the `A`-> `B` dependency is declared with a __non transitive scope__, then `A` won't depend from `C`. 

Projects consuming artifacts coming from Ivy repository can also use `JkScopeMapping` which is more powerful. This notion maps strictly to the [Ivy configuration](http://ant.apache.org/ivy/history/2.2.0/ivyfile/configurations.html) concept.
  
  
### Define a set of dependencies

To define a set dependencies (typically the dependencies of the project to build), you basically define a list of __scoped dependency__ (embodied by `JkScopedDependency`). A __scoped dependency__ is a __dependency__ associated with one or several __scopes__.

Practically, you define some scopes then you bind dependencies to these scopes.

The set of dependency concept is embodied by `JkDependencies` class. This class provides builder for easier instantiation. 

```Java
return JkDependencies.builder()
    .on(GUAVA, "18.0").scope(COMPILE)  
    .on(JERSEY_SERVER, "1.19").scope(COMPILE)
    .on("com.orientechnologies:orientdb-client:2.0.8").scope(COMPILE)
    .on(JUNIT, "4.11").scope(TEST)
    .on(MOCKITO_ALL, "1.9.5").scope(TEST, ANOTHER_SCOPE)
.build();
```

You can also omit the scope and set it later...

```Java
JkDependencies deps = JkDependencies.builder()
    .on(GUAVA, "18.0")
    .on(JERSEY_SERVER, "1.19")
    .on("com.orientechnologies:orientdb-client:2.0.8")
    .on(JUNIT, "4.11").scope(TEST)
    .on(MOCKITO_ALL, "1.9.5").scope(TEST, ANOTHER_SCOPE)
.build();
...
deps = deps.withDefaultScope(COMPILE);
```

If you don't specify scope on a module and you don't set default scope, then at resolution time the dependency will be considerer as binded to every scope. 

<p class="alert alert-success">
<b>Note :</b> Instances of <code>JkDependencies</code> can be added to each other. Look at the <a href="http://jerkar.github.io/javadoc/latest/org/jerkar/api/depmanagement/JkDependencies.html">JkDepencies class API</a> for further details.
</p>

#### Define scopes

In the examples above, we use the predefined scopes `COMPILE` or `TEST`. These scopes are standard scopes defined on the [JkJavaBuild class](https://github.com/jerkar/jerkar/blob/master/org.jerkar.core/src/main/java/org/jerkar/tool/builtins/javabuild/JkJavaBuild.java). 
So if your build definition class inherit from `JkJavaBuild` template you won't need to create it.  

If you need to create your own _scope_, a good practice is to declare it as java constant (`static final`) as it will be reusable anywhere all over your build definition.

As an example, these are the scopes declared in `JkJavaBuild` :

```Java
public static final JkScope PROVIDED = JkScope.of("provided").transitive(false)
    .descr("Dependencies to compile the project but that should not be embedded in produced artifacts.");

public static final JkScope COMPILE = JkScope.of("compile")
    .descr("Dependencies to compile the project.");

public static final JkScope RUNTIME = JkScope.of("runtime").extending(COMPILE)
	.descr("Dependencies to embed in produced artifacts (as war or fat jar files).");

public static final JkScope TEST = JkScope.of("test").extending(RUNTIME, PROVIDED)
	.descr("Dependencies necessary to compile and run tests."); 
```

#### Defining individual dependency

Jerkar can deal with different types of dependency, as simple files located on the filesystem or artifacts located on a Maven/Ivy repositories. 
This section focus on describing the different types of dependencies Jerkar can handle.

##### Dependency on arbitrary files

You just have to mention the path of one or several files. If one of the files does not exist at resolution time (when the dependency is actually retrieved), build fails.

```
    final File depFile1 = new File("/my/file1.jar");
	
    final File depFile2 = new File("/my/file2.zip");

    @Override
    protected JkDependencies dependencies() {
        return JkDependencies.builder()
            .on(depFile1, depFile2).build();
    }
		
``` 

##### Dependency on files produced by computation

It is typically used for __multi projects builds__ or __multi module__ projects.

The principle is that if the specified files are not found, then the computation is run in order to generate the missing files.
If some files still missing after the computation has run, the build fails.

This mechanism is quite simple yet powerful as it addresses following use case :

* Dependency on files produced by other Jerkar project.
* Dependency on files produced by external project built with any type of technology (Ant, Grunt, Maven, Gradle, SBT, Android SDK, Make, ...).
* Dependency on files produced by a method of the main build.   

The generic way is to construct this kind of dependency using a `java.lang.Runnable`.

```
private Runnable computation = new Runnable() {...}; 
	
File fooFile = new File("../otherproject/target/output/foo.jar");  // dependency file  
	
@Override
protected JkDependencies dependencies() {
return JkDependencies.builder()
    .on(JkComputedDependency.of(computation, fooFile)).build();
}
```
Here, if the _fooFile_ is absent then the __computation__ will be run prior to retry to find _FooFile_.

Jerkar provides some shortcuts to deal with other Jerkar projects : For this, you can create the dependency directly from the slave build instance. 

```
@JkProject("../foo")          // The external project path relative to the current project root
public JkJavaBuild fooBuild;  // This is the build coming from the 'foo' project 
	
@Override
protected JkDependencies dependencies() {
    return JkDependencies.builder()
	    .on(fooBuild.asComputedDependency("doPack", fooBuild.packer().jarFile() ))
    .build();
}
```
Here the method `doPack` of `fooBuild` will be invoked if the specified file does not exist.
See _Multi Module Project_ to get details how parameters are propagated to slave builds.

You can also use another kind of project mentioning the command line to run in order to build the project.
 
```
File fooDir = new File("../../foo");  // base dir of a Ant project 
File fooJar = new File(fooDir, "build/foo.jar");
JkProcess antBuild = JkProcess.of("ant", "makeJar").withWorkingDir(fooDir));
...
@Override
protected JkDependencies dependencies() {
    return JkDependencies.builder()
        .on(JkProjectDependency.of(antBuild, fooJar)).scope(PROVIDED)  
    .build();
}
```
Here, if _fooJar_ file does not exist, `ant makeJar` command line is invoked prior to retry to find the file.
If the file still does not exist then the build fails.


##### Dependency on Module

This is for declaring a dependency on module hosted in _Maven_ or _Ivy_ repository. Basically you instantiate a `JkModuleDepency` from it's group, name and version.

```
...	
@Override  
protected JkDependencies dependencies() {
    return JkDependencies.builder()
        .on(GUAVA, "18.0")
        .on("com.orientechnologies:orientdb-client:2.0.8")
        .on("my.group:mymodule:0.2-SNAPSHOT")
	.build();
}
...   
```
There is many way to indicate a module dependency, see [Javadoc](http://jerkar.github.io/javadoc/latest/index.html?org/jerkar/api/depmanagement/JkModuleDependency.html) for browsing possibilities. 

Note that a version ending by `-SNAPSHOT` has a special meaning : Jerkar will consider it _"changing"_. This means that it won't cache it locally and will download the latest version from repository.  

###### Dependencies on Dynamic Versions

Jerkar allows to specify a version range, for example, the following is legal :

```
...	
@Override  
protected JkDependencies dependencies() {
    return JkDependencies.builder()
        .on(GUAVA, "16.+")
        .on("com.orientechnologies:orientdb-client:[2.0.8, 2.1.0[")
	.build();
}
...   
```
As Jerkar relies on Ivy under the hood, you can use any expression mentioned (here) [http://ant.apache.org/ivy/history/latest-milestone/ivyfile/dependency.html].

###### Specifying Maven Classifier and extension of the artifact

If the module repository is a Maven one, you can specify the classifier of the artifact you want to retrieve :

```
...	
@Override 
protected JkDependencies dependencies() {
    return JkDependencies.builder()
        .on("my.group:mymodule:1.0.1:jdk15")
	.build();
}
...   
```

You can also precise the extension of the artifact :

```
...	
@Override 
protected JkDependencies dependencies() {
    return JkDependencies.builder()
        .on("my.group:mymodule:1.0.1:jdk15@zip")
        .on("my.group:otherModule:1.0.15@exe")
	.build();
}
...   
```

 
### Bind Dependencies to Scopes

The whole project dependency description lie in a single instance of `JkDependencies`. This class offers convenient factory methods and builder to define the dependencies.

#### Simple scopes

You can bind any kind of dependency to on one or several scopes as :

```
private static final JkScope FOO = JkScope.of("foo"); 

private static final JkScope BAR = JkScope.of("bar"); 

protected JkDependencies dependencies() {
		return JkDependencies.builder()
		    .on(file("libs/foo3.jar")).scope(BAR)  
		    .on(file("libs/foo1.jar")).scope(BAR, FOO)  
			.on("com.foo:barcomp", "1.19").scope(BAR, FOO)  
			.on("com.google.guava:guava, "18.0")
		.build();
}
```

When the dependency is a __module dependency__, transitive resolution comes in play and more subtle concepts appear.
For resolving __module dependency__ Jerkar uses [__Ivy__](http://ant.apache.org/ivy/) under the cover and scopes are translated to Ivy [_configurations_](http://ant.apache.org/ivy/history/latest-milestone/tutorial/conf.html).
 
So the above module dependencies are translated to Ivy equivalent :

```
...
<dependency org="org.foo" name="barcomp" rev="1.19" conf="bar;foo"/>
<dependency org="com.google.guava" name="guava" rev="18.0"/>
```

#### Scope Mapping

You can also specify a _scope mapping_ (aka _Ivy configuration mapping_) for __module dependencies__ :

```
protected JkDependencies dependencies() {
		return JkDependencies.builder()
			.on("com.foo:barcomp", "1.19")
				.mapScope(COMPILE).to(RUNTIME, BAR)
				.and(FOO, PROVIDED).to("fish", "master(*)")
		.build();
}
```
So the above module dependencies are translated to Ivy equivalent :

```
...
<dependency org="org.foo" name="barcomp" rev="1.19" conf="compile->runtime,bar; foo->fish,master(*); provided->fish,master(*)"/>
```


#### Default Scope Mapping

The way transitive dependencies are actually resolved depends on the `JkDependencyResolver` used for resolution. 
Indeed you can set _default scope_ and _default scope mapping_ on the resolver, through `JkResolutionParameter`. This two settings ends at being translated to respectively _Ivy configuration_ and _Ivy configuration mapping_.
[This page](http://ant.apache.org/ivy/history/2.2.0/ivyfile/configurations.html) explains how _Ivy configurations_ works.




#### Excluding Module from the Dependency Tree

When resolving dependency transitively you may grab unwanted dependencies. To filter them out you can exclude them from the tree using appropriate methods.











 
 
## Dependency Management
----

May the project you are building is standalone : you won't need any library or file coming from other project to build it. In that case you are not concerned with dependency management so you can skip this section.

If your project relies on libraries or files coming from other projects then __dependency management__ comes in play.

### What is a dependency ?

In this context, we call a _dependency_ an indication that can be translated to a set of files. In Jerkar code, the dependency concept is embodied by the abstract `JkDependency` class.
So for example if a project _Foo_ has a _dependency_ on _barDep_, this means that for building, _Foo_ will need the files indicated by _barDep_. 

Jerkar distinguishes 3 types of dependency :

* Arbitrary files located on the file system (Embodied by `JkFileSystemDependency` class,). These files are assumed to be present on the file system when the build is running.
* Files produced by other Jerkar projects (Embodied by `JkProjectDependency` class). These files may be present on file system or not. If they are not present, the external project is built in order to produce the missing files.
* Reference to libraries (Embodied by `JkExternalModuleDependency) hosted in a binary repository (Ivy or Maven for instance) : Jerkar can consume and resolve transitively any artifact located in a repository as you would do with Maven or Ivy.

<p class="alert alert-success">
For the last, Jerkar is using <b>Ivy 2.4.0</b> under the hood. The library is embedded in the Jerkar jar itself and is executed in a dedicated classloader to make the presence of Ivy invisible when you edit code so Ivy classes won't bloat your build path.
</p>

### What is a scope ?

Projects may need dependencies to accomplish certain tasks and needed dependencies may vary according the executed tasks.
For example, to __compile__ you may need _guava_ library only but to __test__ you'll need _junit_ library as well. 
To segregate dependencies according their usage, Jerkar uses the notion of __scope__ (embodied by `JkScope` class). This notion is similar to the Maven scope.

If your project consumes artifacts coming from Ivy repository then you can also use `JkScopeMapping` which is more powerfull. This notion maps strictly to the [Ivy configuration](http://ant.apache.org/ivy/history/2.2.0/ivyfile/configurations.html) concept.
  
  
### Define dependencies for a project

To define dependencies of a project, you basically define a list of __scoped dependency__ (embodied by `JkScopedDependency`).
A scoped dependency is a __dependency__ associated with one or several __scopes__.

So practically, you define some scopes then you bind _dependencies_ to these scopes.

```Java
return JkDependencies.builder()
			.on(GUAVA, "18.0").scope(COMPILE)  
			.on(JERSEY_SERVER, "1.19").scope(COMPILE)
			.on("com.orientechnologies:orientdb-client:2.0.8").scope(COMPILE)
			.on(JUNIT, "4.11").scope(TEST)
			.on(MOCKITO_ALL, "1.9.5").scope(TEST)
		.build();
```

You can also omit the scope and set it later...

```Java
JkDependencies deps = JkDependencies.builder()
			.on(GUAVA, "18.0")
			.on(JERSEY_SERVER, "1.19")
			.on("com.orientechnologies:orientdb-client:2.0.8")
			.on(JUNIT, "4.11").scope(TEST)
			.on(MOCKITO_ALL, "1.9.5").scope(TEST)
		.build();
...
deps = deps.withDefaultScope(COMPILE):
```

Look at the [JkDepencies class API](http://jerkar.github.io/javadoc/latest/org/jerkar/api/depmanagement/JkDependencies.html) to get see all possibilities.


#### Define scopes

In the examples above, we use the predefined scopes `COMPILE` or `TEST`. These scopes are standard scopes defined on the [JkJavaBuild class](https://github.com/jerkar/jerkar/blob/master/org.jerkar.core/src/main/java/org/jerkar/tool/builtins/javabuild/JkJavaBuild.java). 
So if your build definition class inherit from `JkJavaBuild` template you won't need to create it.  

Nevertheless, it's important to know that scopes can inherit from each other. This means that if a _scope A_ inherits from _scope B_ then a dependencies declared with _scope B_ will be also considered as declared with _scope A_.
For example, in `JkJavaBuild` scope `TEST` inherits from `RUNTIME` that inherits from `COMPILE` so every dependencies declared with scope `COMPILE` are considered to be declared with scope `RUNTIME` and `TEST` as well.   

Also know that you can declare a scope to not being _transitive_. In this case the external module dependency transitive dependencies won't be taken in account.
Foe Example, if `A` -> `B` -> `C` and the `A`-> `B` dependency is declared with a non transitive scope, then `A` won't depend from `C`. 

A good practice is to declare __scope__ as java constant (`static final`) as it will be reusable anywhere all over your build definition.

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

Now get focus on each type of dependency we can declare.

##### File system dependencies



##### Project dependencies

This stands for files produced by other project builds. It is typically used in multi-projects (aka multi-module projects).

The principle is that if the specified file are not found, then the dependee project is built in order to generate the missing files.
You can create a __project dependency__ from a Jerkar build defined in another project .` doDefault` method will be invoked on that build to generate missing files.

```
	@JkProject("../foo")          // The external project path relative to the current project root
	public JkJavaBuild fooBuild;  // This is the build coming from the 'foo' project 
	
	@Override
	protected JkDependencies dependencies() {
		return JkDependencies.builder()
			.on(fooBuild.asProjectDependency( fooBuid.packer().jarFile() ))
		.build();
	}
```
If the `fooBuild.packer().jarFile()` is absent then the _foo_ project will be built by invoking the `doDefault` method.

You can also use another kind of project mentioning the command line to run in order to build the project, as such you can easily integrate with Maven, Grunt, SBT, Ant, Gradle projects.
 
```
 	File fooDir = new File("../../foo");  // base dir of a Ant project 
	File fooJar = new File(fooDir, "build/foo.jar");
	JkProcess antBuid = JkProcess.of("ant", "makeJar").withWorkingDir(fooDir));
	...
	@Override
	protected JkDependencies dependencies() {
		return JkDependencies.builder()
			.on(JkProjectDependency.of(antBuild, fooJar))).scope(PROVIDED)  
		.build();
	}
```

This defines a dependency on a file produced by a Ant project. If the file is not present at dependency resolution time, then 
the Ant project is built to produce the missing files.  



#### Bind dependencies to scope

The whole project dependency description lie in a single instance of `JkDependencies`. This class offers convenient factory methods and builder to define the dependencies.

```

```

<br/>










 
 
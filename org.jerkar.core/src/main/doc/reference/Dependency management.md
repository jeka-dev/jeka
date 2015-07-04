## Dependency Management

May the project you are building is standalone : you won't need any library or file coming from other project to build it. In that case you are not concerned with dependency management so you can skip this section.

If your project relies on libraries or files coming from other projects then __dependency management__ comes in play.

Jerkar distinguishes 3 types of dependency :

* Files belonging or produced by other Jerkar projects
* Arbitrary files located on the file system
* Reference to libraries (aka _external module_) hosted in a a binary repository (Ivy or Maven for instance) : this means that Jerkar can consume and resolve transitively any artifact located in a repository as you would with Maven or Ivy.

<p class="alert alert-success">
For the last, Jerkar is using <b>Ivy 2.4.0</b> under the hood. The library is embedded in the Jerkar jar itself and is executed in a dedicated classloader to make the presence of Ivy invisible when you edit code so Ivy classes won't bloat your build path.
</p>

Project may need dependencies to accomplish certain tasks and needed dependencies may vary according the executed tasks.
For example, to __compile__ you may need _guava_ library only but to __test__ you'll need _junit_ library as well. 
To segregate dependencies according their usage, Jerkar uses the notion of __scope__ (embodied by `JkScope` class). This notion is roughly equivalent to the Maven scope.

If your project consumes artifacts coming from Ivy repository then you can also use `JkScopeMapping` which is more powerfull. This notion maps strictly to the [Ivy configuration](http://ant.apache.org/ivy/history/2.2.0/ivyfile/configurations.html) concept.
  
  
### Define scopes

If your build definition class inherit from `JkJavaBuild` template you won't need to create your scope as [this class](https://github.com/jerkar/jerkar/blob/master/org.jerkar.core/src/main/java/org/jerkar/tool/builtins/javabuild/JkJavaBuild.java) declares standard scopes for you.  

Neverthless, know that scopes can inherit from each other. This mean that if a _scope A_ inherits from _scope B_ then a dependencies declared with _scope B_ will be also considered as declared with _scope A_.
For example, in `JkJavaBuild` scope `TEST`inherits from `RUNTIME` that inherits from `COMPILE` so every dependencies declared with scope `COMPILE` are considered to be declared with scope `RUNTIME` and `TEST` as well.   

Know also that you can declare a scope to not being _transitive_. This means that if you declare a external module dependency on such a scope than the transitive dependencies won't be taken in account.

As an example, this is the scope declared in `JkJavaBuild` :

```
public static final JkScope PROVIDED = JkScope.of("provided").transitive(false)
    .descr("Dependencies to compile the project but that should not be embedded in produced artifacts.");

public static final JkScope COMPILE = JkScope.of("compile")
    .descr("Dependencies to compile the project.");

public static final JkScope RUNTIME = JkScope.of("runtime").extending(COMPILE)
	.descr("Dependencies to embed in produced artifacts (as war or fat jar files).");

public static final JkScope TEST = JkScope.of("test").extending(RUNTIME, PROVIDED)
	.descr("Dependencies necessary to compile and run tests."); 
```

### Define dependencies

The entire project dependencies description (dependencies along their respective scopes) stand in a single `JKDependencies` instance. [This class](http://jerkar.github.io/javadoc/latest/org/jerkar/api/depmanagement/JkDependencies.html) provides convenient factories and builder for fluent edition.
 
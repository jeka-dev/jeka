## Master/Slave Projects
-------------------------

Jerkar proposes 2 ways to deal with multi-project builds : 

* By using computed dependencies (see <strong>Dependency Managemment</strong>).
* By defining slave builds. This sections focus on this way.

### Principle

A build class (master build) declares its slave builds. The slave builds can be triggered individually or all-in-one from the master build.
The slave builds are not aware they are slave. In fact any build can be used as slave. The relation is uni-directional 

`JkBuild` defines a method `#slaves()` returning the slaves of its instances (embodied as `org.jerkar.tool.JkSlaveBuilds`). Naturally this result is recursive as it contains slaves of the slaves and so on ...

From this result you can invoke a method for all slaves as `slaves().invokeOnAll("clean")`. The iteration order ensure that an invokation on a build can not be done until all its slaves has been invoked first.  

Also from the command line you can invoke a method or set an option either for the master build only or for the master builds along all its slaves.

### Declare Slave Builds

By default the `#slaves()` method returns all the `JkBuild` instance declared as field and annotated with `@JkProject`. You can modify this behavior by overriding this method.

This is an example of how to declare external build with `@JkProject` annotation.

```
public class DistribAllBuild extends JkBuildDependencySupport {
	
	@JkProject("../org.jerkar.plugins-sonar")
	PluginsSonarBuild pluginsSonar;
	
	@JkProject("../org.jerkar.plugins-jacoco")
	PluginsJacocoBuild pluginsJacoco;
	
```

### Invoke Slave Methods from Master Build Code

To invoke methods on all slaves you can use the `JkSlaveBuilds#invokeOnAll()` method from the instance returned by `JkBuild#slaves()`.

To invoke methods on a single slave, you can just invoke the method on the build instance as `pluginsJacoco.clean()`.

### Invoke Slave Builds from Command Line.

When mentioning a method on the command line, it only applies to the master build. 

If you want this method to be executed on the slave build as well, you must append a `*` at the end of the method name as `jerkar doSomething*`.

If a slave build do not have such a method, the build does not fail but warns it.

### Configure Slave Builds from command line.

When mentioning an option on the command line, only the master build try to inject its corresponding field with the option value.

If you want to inject option field on the slave build as well, just append a `*` at the end of the option declaration as `jerkar aField=myValue*`.

If a build don't have such field, the injection simply does not happen and the build does not fail.

Note that `JkOptions` class is shared among master and slave builds so slave builds can have access to master options by using its static methods.


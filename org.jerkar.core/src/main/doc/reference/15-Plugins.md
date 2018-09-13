## Plugins (Out Dated)
----

Jerkar provides a plugable architecture. To be precise, `org.jerkar.tool.JkBuild` sub-classes are plugable.

A Plugin class must extends `org.jerkar.tool.JkPlugin` and must be named as *JkPlugin[PluginName]* .

When a instance of a plugin is plugged in a `JkBuild` instance :

- The public method of the plugin are available (invokable) from the command line using `jerkar pluginName#MethodName`
- The public instance field values of the plugin can be injected as options from command line using `jerkar -pluginName#FieldName=Xxxx`
- When the plugin is activated, its `activate` method is invoked. This method is supposed to act on the hosting JkBuild instance and its other bound plugins.

Executing `jerkar help` provides an exhaustive list of available plugins in the _build classpath_ and you can have details on each 
by executing `jerkar [pluginName]#help`.


For instance, the Eclipse plugin for `JkJavaBuild` redefines this method to enforce the source directories defined in the _.classpath_ file. 

To bind plugin to a template : you can either declare it inside the build class or mention it in the command line...

### Declare Plugin in Build Class

The best place to declare plugin is within `#init()` method. Indeed, when this method is invoked, fields and project base directory have been already set to proper value.
At this point you can choose either to activate it (mean that the the plugin is always taken in account) or just configure it (in this case the plugin is taken in account only if specified in command line).

To activate a plugin, just invoke `JkBuildPlugins#activate()` method passing the instantiated plugin as :

``` 
@Override
protected void init() {
    JkBuildPluginSonar sonarPlugin = new JkBuildPluginSonar()
        .prop(JkSonar.HOST_URL, sonarEnv.url)
        .prop(JkSonar.BRANCH, "myBranch");
    JkBuildPluginJacoco pluginJacoco = new JkBuildPluginJacoco();
    this.plugins.activate(sonarPlugin);
}
``` 

Here, the SonarQube plugin is active at each build. A SonarQube analysis is run when the `#verify()` method is invoked on the `JkBuild` instance.

But, if you need to configure the plugin without activating it, you must use `JkBuildPlugins#configure()` method instead :

``` 
@Override
protected void init() {
    JkBuildPluginSonar sonarPlugin = new JkBuildPluginSonar()
        .prop(JkSonar.HOST_URL, sonarEnv.url)
        .prop(JkSonar.BRANCH, "myBranch");
    JkBuildPluginJacoco pluginJacoco = new JkBuildPluginJacoco();
    this.plugins.configure(sonarPlugin);
}
``` 

SonarQube plugin is not activated unless you specify `#sonar` in the command line (see below).

### Mention Plugins in the Command Line

You can both configure plugin, activate plugin and invoke plugin methods from the command line without declaring anything in the build definition files.

For such, you need to get the plugin name. By construction the plugin name is plugin class short name minus _JkBuildPlugin_ with lower case at the first letter. 
Indeed, plugin classes are required to be called `JkBuildPluginXxxx` and so `xxxx` is the plugin name for `JkBuildPluginXxxx` plugin class.
If 2 plugins has the same name in your classpath then you have to name it with the fully qualified class name of the plugin class (`xx.xxxx.xx.JkBuildPluginXxxx` for instance).

#### Activate a Plugin

To activate a plugin, you have to mention its name followed by a `#` in the command line. For example if you want to launch unit tests with Jacoco plugin activated, you may execute the following command line : `jerkar doUnitTest jacoco#`.

If a project has slave projects, then you can activate the plugin for both main and slave projects by mentioning a `*` after the plugin declaration as `jerkar doUnitTest jacoco#*`.

#### Configure a Plugin

Configuring a plugin consists in setting its instance fields via the option mechanism. So for setting a plugin field, just mention `-pluginName#fieldName=value` in the command line.

This setting will apply to both master and slave build plugin instances.

#### Execute a Plugin Method

Plugin provide extension point methods to alter template methods but can also provide their own methods. 
To invoke a plugin method, just mention `pluginName#methodName` in the command line. Any public zero argument method is valid.

If a project has slave projects, then you can invoke the plugin method for both main and slave projects by mentioning a `*` after the method invokation as `jerkar myPlugin#doSomething*`.

### Plugins Location

To be activated or configured a plugin has to be in the Jerkar classpath. Plugins are assumed to be packaged as jar file. There is 3 ways to add a plugin in the classpath :

* Add the jar file in _[JERKAR HOME]/libs/ext_. The plugin will be available for all builds within this Jerkar installation but some builds may be not portable to other Jerkar installation. 
* Add the jar file in the _[PROJECT DIR]/build/libs/build_ directory. The build is portable as the plugin jar will be provided with the build definition. If the build has dependencies, they should be provided as well.
* Publish the plugin in a Maven/Ivy repository and mention it in the `@JkImport` annotation. The build is portable as long as the plugin is available in the download repository.

``` 
@JkImport(`{"my.comp:jerkar-plugin-myPlugin:1.1"})
public class MyBuild extends JkJavaBuild {`
...
```

For now, Jerkar ships with several plugins out-of-the-box :
 
 * Eclipse : Leverages and generates Eclipse metadata files (included in _org.jerkar.core.jar_).
 * Idea: Generates .iml and module.xml files for Intellij.
 * Sonar : Executor for SonarQube code analyser (included in _org.jerkar.core-fat.jar_).
 * Jacoco : Test coverage tool (included in _org.jerkar.core-fat.jar_).

You can have a description of plugins available in your classpath by executing `jerkar helpPlugins`.



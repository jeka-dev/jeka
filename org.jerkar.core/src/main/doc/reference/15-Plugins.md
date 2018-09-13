## Plugins (Out Dated)
----

Jerkar provides a plugable architecture. To be precise, build templates provided with Jerkar (`org.jerkar.tool.JkBuild`, `org.jerkar.tool.builtins.javabuild.JkJavaBuild`) are plugable.
They provide methods designed for extension point. Methods designed for extension point alter their behavior according template plugins activated in the enclosing template.



Example for `JkJavaBuild` template : 

```
/**
 * Returns location of production source code (containing edited + generated sources).
 */
public JkFileTreeSet sources() {
    return JkJavaBuildPlugin.applySourceDirs(this.plugins.getActives(),
        editedSources().and(generatedSourceDir()));
}
``` 

By default this method simply returns the files mentioned by the `#editedSources()` and `#generatedSourceDir()`.  If plugins 
are activated, the result may be altered as the `JkJavaBuildPlugin` class specifies :

```
static JkFileTreeSet applySourceDirs(Iterable<? extends JkBuildPlugin> plugins, JkFileTreeSet original) {
    JkFileTreeSet result = original;
        for (final JkBuildPlugin plugin : plugins) {
            result = ((JkJavaBuildPlugin) plugin).alterSourceDirs(result);
        }
    return result;
}
	
/**
 * Override this method if the plugin need to alter the source directory to use for compiling.
 * 
 * @see JkJavaBuild#sources()
 */
protected JkFileTreeSet alterSourceDirs(JkFileTreeSet original) {
    return original;
}
```
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



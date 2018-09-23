## Plugins

Jerkar provides a plugable architecture. To be precise, `org.jerkar.tool.JkBuild` instances are plugable.

A Plugin class must extends `org.jerkar.tool.JkPlugin` and must be named as *JkPlugin[PluginName]* (In fact, plugin name will be inferred from Plugin class name).

When a plugin class is on the classpath : 

- The public methods of the plugin are available (invokable) from the command line using `jerkar pluginName#MethodName`
- The public instance field values of the plugin can be injected as options from command line using `jerkar -pluginName#FieldName=Xxxx`
- If the plugin is loaded, its `activate` method is invoked. This method is supposed to act on the hosting JkBuild instance and its other bound plugins (by modifying the state of these ones).
- A plugin is loaded when :
    * one of its method or option is mentioned in the command line
    * its name followed by a single `#` is mentioned in the command line (e.g. `jerkar scaffold#run java#` loads both scaffold and java plugin)
    * the plugin is loaded programmatically using `JkBuild.plugins.get` methods.

Executing `jerkar help` provides an exhaustive list of available plugins in the _build classpath_ and you can have details on each 
by executing `jerkar [pluginName]#help`.

Plugins may :
- declare _build methods_ .
- override `activate` method (which does nothing by default).
- provide utility classes/methods to build classes or other plugins.

For example, [Jacoco Plugin](https://github.com/jerkar/jerkar/blob/master/org.jerkar.core/src/main/java/org/jerkar/tool/builtins/jacoco/JkPluginJacoco.java) 
does not provide _build method_ but configures Java Plugin in such unit tests are forked on a JVM with Jacoco agent activated. This plugin can be activated 
using the command line `jerkar jacoco#` (No need to declare the plugin into your build class !).
It also provides a utility class `JKocoJunitEnhancer` that supplies lower level features to launch Jacoco programmatically.

An other example is, [Scaffold Plugin](https://github.com/jerkar/jerkar/blob/master/org.jerkar.core/src/main/java/org/jerkar/tool/builtins/scaffold/JkPluginScaffold.java) .
This plugin does not override `activate` method, in such it has no side effect on the running build but it features 
a _build method_ `run` that generates a typical Java project skeleton for Jerkar. This method is executed with `jerkar scaffold#run`.


### Load Plugins in Build Class

A plugin is loaded implicitly in build class by invoking `JkBuild.plugins().get(JkPlugin[pluginName].class)`.
Methods `JkBuild#beforeOptionsInjected` and `JkBuild#afterOptionsInjected` are designated places to declare and configure plugins.

If a plugin has been loaded in methods designated above, its `activate` method will be automatically invoked at build class instantiation time.
If you do not want this, you must declare it in `afterPluginsActivated` method.

Example of loading and configuring a plugin in build class.

```Java
    ...
    @Override
    public void afterOptionsInjected() {
        this.plugins().get(JkPluginSonar.class).prop(JkSonar.BRANCH, "myBranch");
        ...
    }
```


### Add Plugins to Build Classpath

To be activated or configured a plugin has to be in the _build classpath_. 

Therefore it can be mentioned either in command line as `jerkar foo#run @my.comp:jerkar-plugin-foo:1.1 ` or  
in build class code as below : 

``` 
@JkImport("my.comp:jerkar-plugin-myPlugin:1.1")
public class MyBuild extends JkJavaBuild {`
...
```

For now, Jerkar ships with several plugins out-of-the-box. You'll get a comprehensive list by executing `jerkar help`.

### Document Plugins

Plugin writers can embed self-documentation using `@JkDoc` annotation on classes, build methods and public fields.

Writers can also mention that the plugin has dependencies on other plugins using `@JkDocPluginDeps` annotation. This annotation 
has only a documentation purpose and does not has influence on plugin loading mechanism.

A good example is [*Java Plugin*](https://github.com/jerkar/jerkar/blob/master/org.jerkar.core/src/main/java/org/jerkar/tool/builtins/java/JkPluginJava.java)


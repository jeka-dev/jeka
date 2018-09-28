## Plugins

Jerkar provides a plugable architecture. In Jerkar, a plugin is a class extending `org.jerkar.tool.JkPlugin` and named as *JkPlugin[PluginName]*.
The plugin name is inferred from Plugin class name.

Each plugin instance is owned by a JkBuild object, and can access to it through `JkPlugin#build` protected field.

Plugins has 3 capabilities :
* Access to their owning JkBuild instance (so potentially modify it, load/modify other plugins)
* Expose _build methods_ and _options_ to command line.
* Provide self documentation.

Jerkar is bundled with a bunch plugins (java, scaffold, eclipse, intellij, ...) but one can add extra plugins just 
by adding the jar or directory containing the plugin class to your _build classpath_. 

To see all available plugins in the _build classpath_, just execute `jerkar help`.
See [Command Line Parsing](#CommandLineParsing) and [Build Class Pre-processing](#BuildClassPre-processing(Import3rdpartylibraryintoBuildClasspath))
to augment _build_classpath_ .

### Load Plugins

Plugins need not to be mentioned in _build class_ code in order to be bound to the JkBuild instance. Just the fact to 
mention a plugin _build method_, _options_ or _[pluginName]#_ in the command line will load the plugin.

For example `jerkar scaffold#run java#` will load 'java' and 'scaffold' plugins into a JkBuild instance. 
'java' plugin instance will modify 'scaffold' plugin instance in such it produces a build class code extending `JkJavaProjectBuild` 
instead of 'JkBuild' when 'scaffold#run' command is executed. It also creates Java project layout folders. See `activate` method in [JkPluginJava Code](https://github.com/jerkar/jerkar/blob/master/org.jerkar.core/src/main/java/org/jerkar/tool/builtins/java/JkPluginJava.java) 
to have a concrete view.

You can also force a plugin to load in your _build class_ code as below. That way, you don't need to mention `java#` in command line.

```Java
public class MyBuild extends JkBuild {
    
    MyBuild() {
        plugins().get(JkPluginJava.class);  // Loads 'java' plugins in this instance, a second call on 'plugins().get(JkPluginJava.class)' will return same instance.
        plugins().get("intellij");   // You can also load plugin by mentioning its name but it's slower cause it involves classpath scanning
    }
    
}
```

### Modify Owing JkBuild Instance

JkBuild instances are created using `JkBuild#of` factory method. This method invoke `JkPlugin#active` method on all plugin loaded in the JkBuild instance.
By default, this method does nothing but plugin implementations can override it in order to let the plugin modify its owning JkBuild or owe of its plugins.

In fact, many plugins act just as modifier/enhancer of other plugins. 

For example, [Jacoco Plugin](https://github.com/jerkar/jerkar/blob/master/org.jerkar.core/src/main/java/org/jerkar/tool/builtins/jacoco/JkPluginJacoco.java) 
does not provide _build method_ but configures 'java' plugin in such unit tests are forked on a JVM with Jacoco agent on. 
It also provides a utility class `JKocoJunitEnhancer` that supplies lower level features to launch Jacoco programmatically.

Some other plugins does not modify their owning JkBuild instance, for example [Scaffold Plugin](https://github.com/jerkar/jerkar/blob/master/org.jerkar.core/src/main/java/org/jerkar/tool/builtins/scaffold/JkPluginScaffold.java) 
does not override `activate` method, therefore it has no side effect on its owning `JkBuild` instance. It only features _build methods_  along _options_.


### Configure Plugins in JkBuild Class

There is three places where you can configure plugins :
* In `JkBuild` subclass constructor : at this point options has yet been injected so it's the place to configure default option values.
* In `JkBuild#afterOptionsInjected` subclass method : at this point, options has been injected but plugins has not been activated yet. 
  It is the place to configure plugins and other instance member to take options in account.
* In `JkBuild#afterPluginsActivated` subclass method : at this point plugins has been activated. If you wan't to override 
some values plugins may have set, override this method.

Example of configuring a plugin in build class.

```Java
    ...
    public MyBuild() {
        JkPluginSonar sonarPlugin = this.plugins().get(JkPluginSonar.class);  // Load sonar plugin 
		sonarPlugin.prop(JkSonar.BRANCH, "myBranch");  // define a default for sonar.branch property
        ...
    }
```
[Jerkar own build class](https://github.com/jerkar/jerkar/blob/master/org.jerkar.core/build/def/org/jerkar/CoreBuild.java) makes a good example.

### Document Plugins

Plugin writers can embed self-documentation using `@JkDoc` annotation on classes, build methods and public fields.

Writers can also mention that the plugin has dependencies on other plugins using `@JkDocPluginDeps` annotation. This annotation 
has only a documentation purpose and does not has influence on plugin loading mechanism.

A good example is [*Java Plugin*](https://github.com/jerkar/jerkar/blob/master/org.jerkar.core/src/main/java/org/jerkar/tool/builtins/java/JkPluginJava.java)


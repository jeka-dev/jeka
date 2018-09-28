## Self Documentation

_Build classes_ extending `org.jerkar.tool.JkBuild` or `org.jerkar.tool.JkPlugin` can provide self documentation.

When properly auto-documented, users can display the build class or plugin documentation by executing `jerkar help`.

The displayed documentation consist in :
- The Jerkar Built-in option
- A general description of the build class (its purpose). This information is provided by using `@JkDoc` annotation at class level.
- A description of each build method. The description is provided by using `@JkDoc` annotation on _build methods_.
- A description of accept options, with its type and default value. The description is provided by using `@JkDoc` annotation on _build methods_.
- The plugins available in the classpath.

If build class or plugin declare a public instance field without `@JkDoc` annotation, then it will be displayed in help screen but mentioning that there is no description available.

If build class or plugin declare a _build method_ without `@JkDoc`, it will be also displayed in help screen but mentioning that there is no description available.

 This is the display screen for the build class of Jerkar itsef :
 
 ```
 Usage: jerkar [methodA...] [pluginName#methodB...] [-optionName=value...] [-pluginName#optionName=value...] [-DsystemPropName=value...]
 Execute the specified methods defined in build class or plugins using the specified options and system properties.
 When no method specified, 'doDefault' method is invoked.
 Ex: jerkar clean java#pack -java#pack.sources=true -LogVerbose -other=xxx -DmyProp=Xxxx
 
 Built-in options (these options are not specific to a plugin or a build class) :
   -LogVerbose (shorthand -LV) : if true, logs will display 'trace' level logs.
   -LogHeaders (shorthand -LH) : if true, meta-information about the build creation itself and method execution will be logged.
   -LogMaxLength (shorthand -LML) : Console will do a carriage return automatically after N characters are outputted in a single line (ex : -LML=120).
   -BuildClass (shorthand -BC) : Force to use the specified class as the build class to invoke. It can be the short name of the class (without package prefix).
 
 Available methods and options :
 
 From class org.jerkar.CoreBuild :
   Methods :
     doDefault : Conventional method standing for the default operations to perform.
   Options :
     -testSamples (boolean, default : false) : If true, executes black-box tests on sample projects prior ending the distrib.
 
 From class org.jerkar.tool.JkBuild :
   Methods :
     clean : Cleans the output directory.
     help : Displays all available methods defined in this build.
 
 Available plugins in classpath : eclipse, eclipsePath, intellij, jacoco, java, pgp, pom, repo, scaffold, sonar.
 
 Type 'jerkar [pluginName]#help' to get help on a perticular plugin (ex : 'jerkar java#help').
 Type 'jerkar help -Plugins' to get help on all available plugins in the classpath.

 ```
 
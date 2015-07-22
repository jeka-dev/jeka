## Build Configuration
----

Jerkar build are configurable. Build definition classes can retrieve values defined at runtime by reading :

* an environment variable
* a system property
* a Jerkar option

### Environment variables
There is nothing specific to Jerkar. Just set the environment variable as you usually do on your OS and get the value from build using the standard Java `System#getenv` method.

### System properties
Naturally, your build definitions can read system property by using the standard method `System#getProperty`.

Jerkar proposes 3 ways to inject system properties :

* By editing ___[Jerkar Home]/system.properties___ file. All properties defined in this property file are injected while creating the Jerkar Java process (via standard option -D). 
  Note that if you are running Jerkar in embedded mode, the ___[Jerkar Home]/system.properties___ file will not be taken in account but ___[project dir]/build/def/build/system.properties]___.
* By editing ___[Jerkar User Home]/system.properties___ file. These properties are injected the same way than for the previous point.
* By mentioning the property/value in Jerkar __command line__ as `Jerkar doDefault -DmyProperty=myValue`. These properties are injected after the jerkar Java process has been created (via `System#setProperty` method). 

The __command line__ takes precedence on ___[Jerkar User Home]/system.properties___ that in turn, takes precedence on ___[Jerkar Home]/system.properties___.


### Jerkar options

Jerkar options are similar to system properties as it stands for a set of __key/value__. You can read it by using a dedicated API or let it be injected in Java field as explained below.

#### Injecting options

Jerkar proposes 3 ways to inject options :

* By editing ___[Jerkar Home]/options.properties___ file. 
  Note that if you are running Jerkar in embedded mode, the ___[Jerkar Home]/options.properties___ file will not be taken in account but ___[project dir]/build/def/build/options.properties___.
* By editing ___[Jerkar User Home]/options.properties___ file.
* By mentioning the property/value in the Jerkar command line as `Jerkar doDefault -myOption=myValue`.

As for system properties, The __command line__ takes precedence on ___[Jerkar User Home]/options.properties___ that takes in turn,  precedence on ___[Jerkar Home]/options.properties___.

Note for boolean, when no value is specified, `true` will be used as default.

#### Retrieve Jerkar option

You can retrieve string values using the `JkOptions` API providing convenient static methods as `JkOptions#get`, `JkOptions#asMap` or `JkOptions#getAllStartingWith(String prefix)`.

You can also retrieve options just by __declaring fields in build definition class__. 
All non private instance fields of the build definition class, are likely to be injected as an option.

For example, if you declare a field like `protected int size = 3;` then you can override the default value by injecting the option value with any of the 3 ways described above.

Any fields __except static fields or private fields__ can be used to inject options.
If you want __inject option in a private field__, you must annotate it with `@JkDoc` as `@JkDoc private boolean myField;` 

Note that the injected string value will be automatically converted to the target type.
Handled types are __String__, __all primitive types (and their wrappers)__, __enum__, __File__ and __composite object__.
To get a precise idea on how types are converted see [this code](https://github.com/jerkar/jerkar/blob/master/org.jerkar.core/src/main/java/org/jerkar/tool/OptionInjector.java).

#### Composite options

Composite options are a way to structure your options. Say that you want to configure some server access with url, userName and passwsord,
you can gather all these information in a object as 

```Java
class Server {
	private String url;
	private String userName;
	private String password;
	// ...
}
```

declare a Server field in your build :

```Java
class MyBuild extends JkBuild {
	Server deployServer;
	...
}
```
Then you can inject the server object using following options :

```
deployServer.url=http:/myServer:8090/to
deployServer.username=myUsername
deployServer.password=myPassword
```

#### Standard options

Jerkar predefines some standard options that you can set for any build :

* buildClass : This forces the build class to use. If this option is not null then Jerkar will used the specified class as the build class.
Note that this can be a simple class as `MyBuildClass` is enough for running `org.my.project.MyBuildClass`. 
* verbose : when `true` Jerkar will be more verbose at logging at the price of being slower and bloating logs. Default value is `false`.
* silent : when `true`nothing will be logged. Default is `false`


#### How to document options ?

If you want your option been displayed when invoking `jerkar help` you need to annotate it with `@JkDoc`.

For example :

```
@JkDoc("Make the test run in a forked process")
private boolean forkTests = false;
```

<br/>

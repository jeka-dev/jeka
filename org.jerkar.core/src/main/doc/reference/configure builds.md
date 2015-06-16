## Configure builds

Jerkar builds are configurable, in the sense that any value required to run the build has not to be hard-coded 
in the build script. Instead a value can be injected by 3 different means :
* Environment variable
* System properties
* Jerkar options

### Environment variables
There is nothing specific to Jerkar. Just set the environment variable as you usually do on your OS and get the value from build using the standard Java `System#getenv` method.

### System properties
Jerkar proposes 3 ways to inject system properties :
* By setting the property/value in the `system.properties` file located in the `[Jerkar Home]` directory. 
  Note that if you are running Jerkar in embedded mode, the `[Jerkar Home]/system.properties` file will not be taken in account but [project dir/build/def/build/system.properties].
* By setting the property/value in the `system.properties` file located in the `[Jerkar User Home]` directory. The Jerkar user home is mentioned at the beginning of all Jerkar process.
* By mentioning the property/value in the Jerkar command line as `Jerkar doDefault -DmyProperty=myValue

The command line takes precedence on the `[Jerkar User Home]/system.properties` that takes precedence, in turn, on the [Jerkar Home]/system.properties.

To read system properties from your builds, you can just use the standard method `System#getProperty`.

### Jerkar options
Jerkar options are similar to system properties as it stands for a set of key/values. Jerkar proposes 3 ways to inject Jerkar properties :
* By setting the property/value in the `options.properties` file located in the `[Jerkar Home]` directory. 
  Note that if you are running Jerkar in embedded mode, the `[Jerkar Home]/options.properties` file will not be taken in account but [project dir/build/def/build/options.properties].
* By setting the property/value in the options.properties file located in the `[Jerkar User Home]` directory. The Jerkar user home is mentioned at the beginning of all Jerkar process.
* By mentioning the property/value in the Jerkar command line as `Jerkar doDefault -myOption=myValue

As for system properties, The command line takes precedence on the `[Jerkar User Home]/options.properties` that takes precedence, in turn, on the [Jerkar Home]/options.properties.

Note for boolean, when no value is specified, `true` will be used as default.

#### Standars options

Jerkar predefines some standard options that you can set for any build :
* buildClass : This forces the build class to use. If this option is not null then Jerkar will used the specified class as the build class.
Note that this can be a simple class as `MyBuildClass` is enough for running `org.my.project.MyBuildClass`. 
* verbose : when `true` Jerkar will be more verbose at logging at the price of being slower and bloating logs. Default value is `false`.
* silent : when `true`nothing will be logged. Default is `false`


#### How to retrieve Jerkar options ?

You can retrieve string values using the `JkOptions` API providing convenient static methods as `JkOptions#get`, `JkOptions#asMap` or `JkOptions#getAllStartingWith(String prefix)`.

You can also retrieve an option just by declaring a field in the build class having the same name as the option.
For example, if you declare a field like `protected int size = 3;` then you can override the default value by injecting the option value in any of the 3 way described above.

Any field except static fields or private fields can be used to inject options.
If you want inject option in a  private field, you must annotate is with @JkDoc as `@JkDoc private boolean myField;` 

Note that the string value that you have injected will be automatically converted to the target type.
Handled types are String, all primitive types (and their wrappers), enum, File and composite object.

To get a precise idea on how types are converted see [this code](https://github.com/jerkar/jerkar/blob/master/org.jerkar.core/src/main/java/org/jerkar/OptionInjector.java).

#### Composite options

Composite options are a way to structure your options. Say that you want to configure some server access with url, userName and passwsord,
you can gather all these information in a object as 
```
class Server {
	private String url;
	private String userName;
	private String password;
	...
}
```

declare a Server field in your build
```
class MyBuild extends JkBuild {
	Server deployServer;
	...
}
```
Then you can inject the server object using following options :
deployServer.url=http://myServer:8090/to
deployServer.username=myUsername
deployServer.password=myPassword


#### How to document option ?

If you want your option been displayed when invoking `jerkar help` you need to annotate it with `@JkDoc`.

For example :
```
@JkDoc("Make the test run in a forked process")
private boolean forkTests = false;
```

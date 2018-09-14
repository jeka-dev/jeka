## Build Configuration

Jerkar builds are configurable. Build classes can retrieve values defined at runtime by reading :

* environment variables
* system properties
* Jerkar options

### Environment Variables
There is nothing specific to Jerkar. Just set the environment variables as you usually do on your OS and get 
the value in your build classes using the standard `System#getenv` method.

### System Properties
As for environment variables, your build classes can read system properties using the standard `System#getProperty` method.

Jerkar proposes 3 ways to inject system properties. They are considered in following order :

* Properties mentioned in Jerkar command line as `Jerkar doDefault -DmyProperty=myValue`.
* Properties mentioned in ___[Jerkar User Home]/system.properties___ file. 
* Properties mentioned in ___[Jerkar Home]/system.properties___ file. 
  Note that if you are running Jerkar in embedded mode, the ___[Jerkar Home]/system.properties___ file will not be taken in account but ___[project dir]/build/boot/system.properties___.

In every case, defined system properties are injected after the creation of the java process (via `System#setProperty` method).

### Jerkar Options

Jerkar options are similar to system properties as it stands for a set of key/value. 

Options are globally available in all build classes but can be retrieve in a static typed way (injected in build class fields) 
or as set of key/string value. 

#### Inject Options

Jerkar proposes 3 ways to inject options. They are considered in following order :

* Options mentioned in Jerkar command line as `Jerkar doDefault -myOption=myValue`.
* Options mentioned in ___[Jerkar User Home]/options.properties___ file.
* Options mentioned in ___[Jerkar Home]/options.properties___ file. 
  Note that if you are running Jerkar in embedded mode, the ___[Jerkar Home]/options.properties___ file will not be taken in account but ___[project dir]/build/boot/options.properties___.

Note for boolean options, when no value is specified, `true` will be used as default.

#### Retrieve Options as String Values

You can retrieve string values using the `JkOptions` API providing convenient static methods as `JkOptions#get`, `JkOptions#getAll` or `JkOptions#getAllStartingWith(String prefix)`.

This way you only get the string literal value for the option and you have to parse it if the intended type was a boolean or a number.

#### Retrieve Option in Build Class Fields

You can retrieve options just by declaring fields in build classes. 
All public non-final instance fields of the invoked build class, are likely to be injected as an option.

For example, if you declare a field like :

```
class MyBuild extends JkBuild {
   public int size = 10;
   ...
}
``` 
Then you can override the value by mentioning in command line `jerkar doSomething -size=5`.

Note that the injected string value will be automatically converted to the target type.

Handled types are : _String_, _all primitive types (and their wrappers)_, _enum_, _File_ and _composite object_.
If the value is not parsable to the target type, build fails. 

To get a precise idea on how types are converted see [this code](https://github.com/jerkar/jerkar/blob/master/org.jerkar.core/src/main/java/org/jerkar/tool/OptionInjector.java).

##### Composite options

Composite options are a way to structure your options. Say that you want to configure some server access with url, userName and passwsord. 
You can group all these information into a single object as :

```Java
class Server {
    public String url;
    public String userName;
    public String password;
    // ...
}
```

Declare a Server field in your build :

```Java
class MyBuild extends JkBuild {
   public Server deployServer;
   ...
}
```
Then you can inject the server object using following options :

```
deployServer.url=http:/myServer:8090/to
deployServer.username=myUsername
deployServer.password=myPassword
```

#### Document Options

If you want your option been displayed when invoking `jerkar help` you need to annotate it with `@JkDoc`.

For example :

```
@JkDoc("Make the test run in a forked process")
public boolean forkTests = false;
```

#### Built-in Options

Jerkar defines some built-in options that are used by the engine itself. Unlike regular options, they respect an UpperCamelCase naming
convention :

- -LogVerbose (shorthand -LV) : if true, logs will display 'trace' level logs.
- -LogHeaders (shorthand -LH) : if true, meta-information about the build creation itself and method execution will be logged.
- -LogMaxLength (shorthand -LML) : Console will do a carriage return automatically after N characters are outputted in a single line (ex : -LML=120).
- -BuildClass (shorthand -BC) : Force to use the specified class as the build class to invoke. It can be the short name of the class (without package prefix).



You must have a JDK 8 or higher installed on your machine. 
To determine the JDK to run upon, _jeka_ prioritizes the following, in order :

* _JEKA_JDK_ environment variable ([_JEKA_JDK_]/bin/java must point on _java_ executable)
* _jeka.jdk.XX_ property definied in _[JEKA_USER_DIR]/global.properties_, if a _jeka.java.version=XX_ property is defined in _[PROJECT_ROOT]/jeka/local.properties_ file.
* _JAVA_HOME_ environment variable 
*  _java_ executable accessible from _PATH_ environment variable.

!!! note
    _Jeka_ organization provides a [plugin](https://plugins.jetbrains.com/plugin/13489-jeka/) for IntelliJ in order to render the following tasks useless. 

    Here, we'll focus only on how to do it using command line only.

There are two ways of using _jeka_ : by installing _Jeka_ itself, or by using a project that already contains the _Jeka_ wrapper.

## Install Jeka Distribution

!!! note
    It is not required to install _Jeka_ distribution to use it. If you fetch a project that already contains a wrapper, using `jekaw` will be enough.
    Nevertheless, if you want to create a project from scratch, installation will be necessary.

* Download the latest release [here](https://search.maven.org/search?q=g:%22dev.jeka%22%20AND%20a:%22jeka-core%22)
  and unpack the _distrib.zip_ wherever you want on your machine. We'll refer to this directory as [JEKA HOME].
* Add [JEKA HOME] to your _PATH_ environment variable.
* Now, you can use the `jeka` command from everywhere.

## Projects with Jeka Wrapper

There is no need to install Jeka to work a project already holding a Jeka wrapper:

* Fetch the project from its remote repository
* Use the `jekaw` command from the project root path.


!!! note
    To start from an empty project, clone the blank project template from _https://github.com/jerkar/jeka-wrapper-project-template.git_


## Setup IDE

* Add an IDE _path variable_ ***JEKA_USER_HOME*** pointing on _[USER HOME]/.jeka_. In _Intellij_ :  **Settings... | Appearance & Behavior | Path Variables**
* If you use `jeka` instead of `jekaw`, add an IDE _path variable_ ***JEKA_HOME*** pointing on [JEKA HOME].

## Hello World Project 

For simplicity's sake, we'll use the basic example of displaying a message on the console.

### Create Project Structure

* Create the root dir of the project or use the _template wrapper project_ mentioned above. 
* At project root, execute `jeka scaffold#run scaffold#wrapper` (or just `jekaw scaffold#run` if using the template wrapper project).
* Execute `jeka intellij#iml` or `jeka eclipse#files` in order to generate the IDE configuration file. 
* The project with a _Jeka_ structure and a basic build class is ready to work within your IDE

### Create a Jeka Method

Jeka methods must be zero-args instance methods returning `void`. 

Modify the _def/Build.java_ file to add the _hello_ method as follows.


```Java
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkInit;

class Build extends JkBean {

    public void hello() {
        System.out.println("Hello world !");
    }

    public static void main(String[] args) throws Exception {
        Build build = JkInit.instanceOf(Build.class, args);
        build.hello();
    }

}
```

Now, execute `jeka hello` to see the message displayed on the console.

Optionally, you can also use the classical `main` method to launch it from the IDE.  


### Get Contextual Help

Execute `jeka -h` (or `jeka -help`) to display contextual help on the console.

Follow the instructions to navigate to more specific help.

!!! warning
    Do not confuse `jeka -help` and `jeka help`. The latter only provides documentation about the default _KBean_.

## Customization

For any reason, we might use different locations than the standard ones for _Jeka User Home_ and _Jeka Cache_.

We can change the location using the following OS environment variables :

  * `JEKA_USER_HOME` : to set the _Jeka User Home_ to a specified location (absolute path).
  * `JEKA_CACHE_DIR` : to set the cache (mainly all downloaded files), to a specified location (absolute path)



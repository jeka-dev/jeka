You need a JDK 8 or higher installed on your machine. 
To determine the JDK to run upon, _jeka_ looks in priority order :

* _JEKA_JDK_ environment variable ([_JEKA_JDK_]/bin/java must point on _java_ executable)
* _JAVA_HOME_ environment variable 
*  _java_ executable accessible from _PATH_ environment variable.

!!! note
    _Jeka_ organization provides a [plugin](https://plugins.jetbrains.com/plugin/13489-jeka/) for IntelliJ in order to make the following tasks useless. 

    Here, we'll focus only on how to do it using command line only.

There are two ways of using _jeka_ : by installing _Jeka_ itself or by using a project already containing the _Jeka_ wrapper.

## Install Jeka Distribution

!!! note
    It is not required to install _Jeka_ distribution to use it. If you fetch a project already holding a wrapper, using `jekaw` would be enough.
    Nevertheless, if you want to create project from scractch, it will be required.

* Download the latest release from [here](https://search.maven.org/search?q=g:%22dev.jeka%22%20AND%20a:%22jeka-core%22)
  and unpack the _distrib.zip_ wherever you want on your machine. We'll refer to this directory as [JEKA HOME].
* Add [JEKA HOME] to your _PATH_ environment variable.
* Now, you can use `jeka` command from everywhere.

## Projects with Jeka Wrapper

For working on a project already holding a Jeka wrapper, there is no need to install Jeka :

* Fetch the project from its remote repository
* Use `jekaw` command from project root path.


!!! note
    To start from an empty project, clone template blank project from _https://github.com/jerkar/jeka-wrapper-project-template.git_


## Setup IDE

* Add an IDE _path variable_ ***JEKA_USER_HOME*** pointing on _[USER HOME]/.jeka_. In _Intellij_ :  **Settings... | Appearance & Behavior | Path Variables**
* If you use `jeka` instead of `jekaw`, add an IDE _path variable_ ***JEKA_HOME*** pointing on [JEKA HOME].

## Hello World Project 

For simplicity's sake, we'll use trivial example just displaying a message on console.

### Create Project Structure

* Create the root dir of the project or use the _template wrapper project_ mentioned above. 
* At project root, execute `jeka scaffold#run scaffold#wrapper` (or just `jekaw scaffold#run` if using the template wrapper project).
* Execute `jeka intellij#iml` or `jeka eclipse#files` in order to generate IDE configuration file. 
* The project with a _Jeka_ structure and a basic build class is ready to work within your IDE

### Create a Jeka Method

Jeka methods require to be zero-args instance methods returning `void`. 

Modify the _def/Build.java_ file to ad _hello_ method as follow.


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

Now, execute `jeka hello` to see message displayed on console.

Optionally, we can use a classical `main` method to launch it from the IDE.  


### Get Contextual Help

Execute `jeka -h` (or `jeka -help`) to display a contextual help on the console.

Follow instructions to navigate to more specific help context.

!!! warning
    Do not confuse `jeka -help` and `jeka help`. The last provides only documentation about the default _KBean_.

## Customization

For any reasons, we might use different location than the standard ones for _Jeka User Home_ and _Jeka Cache_.

We can change the location using following OS environment variable :

  * `JEKA_USER_HOME` : to set the _Jeka User Home_ to a specified location (absolute path).
  * `JEKA_CACHE_DIR` : to set the cache (mainly all downloaded files), to a specified location (absolute path)



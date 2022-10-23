## Properties

_Properties_ are pairs of String  _key-value_ that are used across Jeka system. It typically carries urls, local paths,
tool versions or credentials. 

_Properties_ can be defined at different level, in order of precedence :

* System properties : Properties can be defined using system properties as `-DpropertyName=value`. System properties can
  be injected from Jeka command line.
* OS environment variables : Properties can also be defined as OS environment variable.
* Project : Defined in _[Project Root]/jeka/project.properties_. Used typically for storing tool version (e.g. `jeka.kotlin.version=1.5.21`).
* Global : Defined in _[User Home]/.jeka/global.properties_ file. Used typically to define urls, local paths and credentials.


_Properties_ inherit from project _properties_ defined in project parent folders (if exists). 

Here, project2 will inherit properties defined in _project1/jeka/project.properties_ :
```
project1
   + jeka
      + project.properties
   + project2   (sub-project)
      + jeka
         + project.properties
```

!!! info
    _Properties_ support interpolation via `${}`tokens. 
    
    For example, if we define the following properties :
    `foo=fooValue` and `bar=bar ${foo}` then `JkProperties.get("bar")` will return 'bar fooValue'.

### Standard properties

* `jeka.jdk.X=` location of the JDK version X _(e.g. jeka.jdk.11=/my/java/jdk11)_. It is used to compile projects when 
  project JVM target version differs from Jeka running version.
* `jeka.kotlin.version` : Version of Kotlin used for compiling both _def_ and Kotlin project sources.
* `jeka.java.version` :  Target JVM version for compiled files.

### Command shorthands

* `jeka.cmd.xxx=` define an alias that can be substituted to its value in the command line using `:` symbol.
    Example : `jeka.cmd.myBuild=${jeka.cmd.build} sonarqube#run jacoco#` lets to simply execute `jeka :myBuild`.
*  `jeka.cmd._append=` will append the argument  to every Jeka execution command.
   Example : `jeka.cmd._append=@dev.jeka:springboot-plugin` will add springboot plugin to Jeka classpath for each execution.

!!! Note
    Command shorthands is a really powerful mechanism to get rid of build classes.
    Many projects can be builds using properties only !

### Repositories

The repositories used to download and publish artifacts can be defined using _properties_.
The download repositories are set using `jeka.repos.download` property, while publish repository is defined using `jeka.repos.publish`.

Use [JkRepoFromProperties class](https://github.com/jeka-dev/jeka/blob/master/dev.jeka.core/src/main/java/dev/jeka/core/api/depmanagement/JkRepoFromProperties.java)
to get the repositories defined by _properties_.

!!! Note
    By default, when no repository is configred, artifacts are downloaded on _Maven Central_ repo.

Using single repo
```
jeka.repos.download=https://my.company/repo
```

Using multiple repos
```
jeka.repos.download=https://my.company/repo1, file://path/to/a/local/repo 
```

Using single repo with credential
```
jeka.repos.download=https://my.company/repo
jeka.repos.download.username=myUsername
jeka.repos.download.password=myPassword
```

For convenience, we can define multiple repositories and reference them using aliases
```
jeka.repos.myRepo1=https://my.company/repo
jeka.repos.myRepo1.username=myUsername
jeka.repos.myRepo1.password=myPassword

jeka.repos.myRepo2=https://my.company/repo2
jeka.repos.myRepo2.username=myUsername2
jeka.repos.myRepo2.password=myPassword2

jeka.repos.download=myRepo1, myRepo2
jeka.repos.publish=myRepo2
```

Aliases exist for _Maven Central_ and Jeka GitHub Repo
```
jeka.repos.download=https://my.company/repo1, mavenCentral, jekaGithub
jeka.repos.jekaGithub.username=myGithubAccountName
jeka.repos.jekaGithub.password=myGithubPersonalAccessToken
```

### KBean field value injection

If a property is named as `xxx#yyyyy` then Jeka will try to inject its value 
in public field `yyyyy` of KBean `xxx`. 

examples:
```
springboot#springbootVersion=2.4.7
project#test.skip=true
```

!!! Note
    There is a slight difference between using `-Dproject#test.skip=true` and 
    `project#test.skip=true` on the command line.<br/>
    For the first, field is injected via system properties, this means that for multi-modules projects,
    the value will be injected on every Jeka module.
    While for the second, the value will be injected only on the root module.









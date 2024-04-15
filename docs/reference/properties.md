## Properties

_Properties_ are pairs of String  _key-value_ that are used across Jeka system. They typically carry urls, local paths,
tool versions, or credentials. 

_Properties_ can be defined at different levels, in order of precedence :

* System properties : Properties can be defined using system properties as `-DpropertyName=value`. System properties can
  be injected from the Jeka command line.
* OS environment variables : Properties can also be defined as OS environment variables.
* Project : Defined in _[Project Root]/jeka.properties_. Typically used to store tool versions (e.g. `jeka.kotlin.version=1.5.21`).
* Global : Defined in the _[User Home]/.jeka/global.properties_ file. Typically used to define urls, local paths, and credentials.


_Properties_ inherit from project _properties_ defined in project parent folders (if extant). 

Here, project2 will inherit properties defined in _project1/jeka/local.properties_ :
```
project1
   + jeka.properties
   + project2   (sub-project)
      + jeka.properties
```

!!! info
    _Properties_ support interpolation via `${}`tokens. 
    
    For example, if we define the following properties :
    `foo=fooValue` and `bar=bar ${foo}` then `JkProperties.get("bar")` will return 'bar fooValue'.

### Standard properties

* `jeka.jdk.X=` location of the JDK version X _(e.g. jeka.jdk.11=/my/java/jdk11)_. It is used to compile projects when 
  project JVM target version differs from Jeka running version.
* `jeka.kotlin.version` : Version of Kotlin used to compile both _def_ and Kotlin project sources.
* `jeka.java.version` :  Target JVM version for compiled files.
* `jeka.classpath.inject`: Additional dependencies that will be added to the JeKa classpath. 
   We can specify many dependencies separated by `<space>`.
   It can be either Maven coordinates or file paths. If a file path is relative, it is resolved 
   upon project base dir (could be distinct from working dir).
   Example : `jeka.classpath.inject=dev.jeka:springboot-plugin  com.google.guava:guava:31.1-jre ../other-project/jeka/output/other-project.jar`
* `jeka.default.kbean`: The KBean to use when omitting mentioning KBean prefix (or using `kb#` prefix) in command or field assignment.
   Example: declaring `jeka.default.kbean=myBean`, makes the following expressions equivalent : `myBean#run`, `#run`, and `kb#run`.

### Command shorthands

* `jeka.cmd.xxx=` define an alias that can be substituted for its value in the command line using the `:` symbol.
    Example : `jeka.cmd.myBuild=${jeka.cmd.build} sonarqube#run jacoco#` allows you to simply execute `jeka :myBuild`.
*  `jeka.cmd._append=` will append the argument to every Jeka execution command.
   Example : `jeka.cmd._append=@dev.jeka:springboot-plugin` will add springboot plugin to Jeka classpath for each execution.
   This properties can be splitted when argument line becomes to long. In fact, every properties starting with `jeka.cmd._append` will 
   be taken in account to assemble the extra command line arguments. For example, we can define `jeka.cmd._append.0=`, `jeka.cmd._append.1=`, and so on.

!!! Note
    Command shorthands are a really powerful mechanism for getting rid of build classes.
    Many projects can be built using properties only !

### Repositories

The repositories used to download and publish artifacts can be defined using _properties_.
The download repositories are set using the `jeka.repos.download` property, while the publish repository is defined using `jeka.repos.publish`.

Use [JkRepoFromProperties class](https://github.com/jeka-dev/jeka/blob/master/dev.jeka.core/src/main/java/dev/jeka/core/api/depmanagement/JkRepoFromProperties.java)
to get the repositories defined by _properties_.

!!! Note
    By default, when no repository is configured, artifacts are downloaded on _Maven Central_ repo.

Using single repo
```
jeka.repos.download=https://my.company/repo
```

Using multiple repos
```
jeka.repos.download=https://my.company/repo1, file://path/to/a/local/repo 
```

Using single repo with credentials
```
jeka.repos.download=https://my.company/repo
jeka.repos.download.username=myUsername
jeka.repos.download.password=myPassword
```

Specifying http headers to include in each request towards the repo
```
jeka.repos.download.headers.my-header-name=myHeaderValue
jeka.repos.download.headers.another-header-name=anotherHeaderValue
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

Aliases are predefined for _Maven Central_ and Jeka GitHub Repos
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
@springboot.springbootVersion=2.4.7
@project.test.skip=true
```

!!! Note
    There is a slight difference between using `-D@project.test.skip=true` and 
    `@project.test.skip=true` in the command line.<br/>
    For the former, the field is injected via system properties, this means that for multi-modules projects,
    the value will be injected on every Jeka module.
    For the latter, the value will be injected only on the root module.









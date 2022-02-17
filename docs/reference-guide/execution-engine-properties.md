## Properties

Properties are pairs of String  _key-value_ that are used across Jeka system. It typically carries urls, local paths,
tool versions or credentials. They can be globally accessed using `JkProperties#get*` static method.

Properties can be defined at different level, in order of precedence :

* System properties : Properties can be defined using system properties as `-DpropertyName=value`. System properties can
  be injected from Jeka command line.
* OS environment variables : Properties can also be defined as OS environment variable.
* Project : Defined in _[Project Root]/jeka/project.properties_. Used typically for storing tool version (e.g. `jeka.kotlin.version=1.5.21`).
* Global : Defined in _[User Home]/.jeka/global.properties_ file. Used typically to define urls, local paths and credentials.


Properties inherit from project properties defined in project parent folders if exists. Here, project2 will 
inherit properties defined in _project1/jeka/project.properties_ :
    
```
project1
   + jeka
      + project.properties
   + project2
      + project.properities.
```

### Standard properties

* `jeka.jdk.X=` location of the JDK version X _(e.g. jeka.jdk.11=/my/java/jdk11)_. It is used to compile projects when 
  project JVM target version differs from Jeka running version.
* `jeka.kotlin.version` : Version of Kotlin used for compiling both _def_ and Kotlin project sources.

### Repositories

The repositories used to download and publish artifacts can be defined using properties.
The download repositories are set using `jeka.repos.dowload` property, while publish repository is defined using `jeka.repos.publish`.

Use [this class](https://github.com/jeka-dev/jeka/blob/master/dev.jeka.core/src/main/java/dev/jeka/core/api/depmanagement/JkRepoFromProperties.java)
to get the repository defined by properties.

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

Aliases exist for _Maven Central_ and Jeka Github Repo
```
jeka.repos.download=https://my.company/repo1, mavenCentral, jekaGithub
jeka.repos.jekaGithub.username=myGithubAccountName
jeka.repos.jekaGithub.password=myGithubPersonalAccessToken
```






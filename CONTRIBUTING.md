Jerkar welcomes contributors. As a new project there's plenty of free rooms to start : You can extends/debug the jerkar project itself but you van also write addin/plugin for integrate better Jerkar with your favorite technology. Don't be intimidated, it's relatively easy and you can provide great added value just by writing very few code. As an example, look at the [Spring Boot addin](https://github.com/jerkar/spring-boot-addin). 
Also do not hesitate to [contact contributors](https://github.com/djeang) to discuss about what is best to start with.

## Coding rule
If you contribute to Jerkar Core project, there's only 1 rule : try to mimic the current style :-).
More concretely :

* Make a class public only when really needed. If a class is public, it should be prefixed with `Jk` (The goal is to not pollute auto-completion in IDE when Jerkar is on the build path).
* Favor immutable objects when reasonable
* Embrace a fluent style API
* Don't use 3rd party dependencies (Use or enrich JkUtilsXxxxx classes for commons). 
* Jerkar 0.7.x relies on JDK8

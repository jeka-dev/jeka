Jeka contains a library for all regular things you need to build/test/publish projects..
The library does not depend on the execution engine and has zero dependency. 

## API Style

_Jeka_ tries to stick with a consistent API design style.

* All Jeka public classes/interfaces start with `Jk`. The reason is for easing distinction, in IDE, between classes supposed be used
  in production or test and the ones used for building. It also helps to explore Jeka API.
* As a rule of thumb _Jeka_ favors immutable objects for shallow structures and
[parent-chaining trees](https://github.com/djeang/parent-chaining/blob/master/readme.md) for deeper ones.
Both provide a fluent interface when possible.
* All objects are instantiated using static factory methods. Every factory method names start with `of`.
* All accessor method names (methods returning a result without requiring IO, only computation) starts with `get`.
* To create a subtly different object from another immutable one, _Jeka_ provides :
  * Methods starting with `with` when a property is to be replaced by another.
  * Methods starting with `and` when a collection property is to be replaced by the same one plus an extra element.
  * Methods starting with `minus` when a collection property is to be replaced by the same one minus a specified element.
* To modify a mutable object, _Jeka_ provides :
  * Methods starting with `set` to replace a single property value by another.
  * Methods starting with `add` to add a value to a collection property.
  Those methods return the object itself for chaining.

## Domains Covered by the API

The previous example demonstrates how the Java/project API can be used to build and publish Java projects. This API
relies on other lower level ones provided by _Jeka_. In a glance these are the domains covered by the _Jeka_ APIs :

* __Files :__ File trees, filters, zip, path sequence
* __System :__ Launching external process, Logging, Meta-info
* __Cryptography :__ PGP signer
* __Dependency management :__ Dependency management, publishing on repositories
* __Java :__ Compilation, javadoc, resource processor, manifest, packager, classloader, classpath, launching
  * __Testing :__ Launching tests and get reports
  * __Project :__ Project structure to build
* __Tooling :__ Eclipse integration, intellij integration, Maven interaction, Git
* __Support :__ Set of utility class with static methods to handle low-level concerns


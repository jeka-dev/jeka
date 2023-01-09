Jeka contains a library for all regular things required to build/test/publish projects.
The library does not depend on the execution engine and has zero dependency. 

## API Style

_Jeka_ tries to stick with a consistent API design style.

* All Jeka public classes/interfaces start with `Jk`. The reason for this is to ease distinction, in IDE, between classes supposed be used
  in productions or tests and the ones used for building. It also helps to explore Jeka API.
* As a rule of thumb, _Jeka_ favors immutable objects for shallow structures and
mutable ojects for deeper structures.
Both provide a fluent interface when possible.
* In deep structures, final fields are declared `public` and have no getter counterpart.
* All objects are instantiated using static factory methods. Every factory method name starts with `of`.
* All accessor method names (methods returning a result without requiring IO, meaning computation only) start with `get`.
* To create a subtly different object from an immutable one, _Jeka_ provides :
    * Methods starting with `with` when a property is to be replaced by another.
    * Methods starting with `and` when a collection property is to be replaced by the same one plus an extra element.
    * Methods starting with `minus` when a collection property is to be replaced by the same one minus a specified element.
* To modify a mutable object, _Jeka_ provides :
    * Methods starting with `set` to replace a single property value with another.
    * Methods starting with `add` to add a value to a collection property.
    * Those methods return the object itself for chaining.

## Domains Covered by the API

The previous example demonstrates how the Java/project API can be used to build and publish Java projects. This API
relies on other lower level ones provided by _Jeka_. At a glance, these are the domains covered by the _Jeka_ APIs :

* __Files :__ File trees, filters, zip, path sequence
* __System :__ Launching external process, Logging, Meta-info
* __Cryptography :__ PGP signer
* __Dependency management :__ Dependency management, publishing on repositories
* __Java :__ Compilation, javadoc, resource processor, manifest, packager, classloader, classpath, launching
    * __Testing :__ Launching tests and get reports
    * __Project :__ Project structure to be built
* __Tooling :__ Eclipse integration, intellij integration, Maven interaction, Git
* __Support :__ Set of utility class with static methods to handle low-level concerns


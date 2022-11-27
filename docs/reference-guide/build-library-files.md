## Files

File manipulation is a central part of building software.
Jeka embraces JDK7 *java.nio.file* API by adding some concepts around it, to provide a powerful and fluent style of API performing
recurrent tasks with minimal effort.

The following classes lie in the `dev.jeka.core.api.file` package:

* `JkPathFile` A simple wrapper for files (not folders). It provides copying, interpolation, checksum, deletion and creation methods.

* `JkPathSequence` An Immutable sequence of `java.nio.file.Path` providing methods for filtering or appending.

* `JkPathMatcher` An immutable `java.nio.file.PathMatcher` based on `java.nio.file` glob pattern or regerxp.
  Used by `JkPathTree` to filter in/out files according name patterns.

* `JkPathTree` An Immutable root folder along a `PathMatcher` providing operations to copy, navigate, zip, or iterate.
  This is a central class in Jeka API.

* `JkZipTree` Same as `JkPathTree` but using a zip file instead of a directory. It allows you to manipulate a zip file
  as a regular folder.

* `JkPathTreeSet` An Immutable set of `JkPathTree`. Helpful to define a set of sources/resources and create jar/zip files.

* `JkResourceProcessor` A mutable processor for copying a set of files, preserving the structure and
  replacing some texts with others. Typically used for replacing the token `${server.ip}` with an actual value.

Examples

```java
// creates a file and writes the content of the specified url.
JkPathFile.of("config/my-config.xml").createIfNotExist().replaceContentBy("http://myserver/conf/central.xml");

// copies all non java source files to another directory preserving structure
JkPathTree.of("src").andMatching(false, "**/*.java").copyTo("build/classes");

// One liner to zip an entire directory
JkPathTree.of("build/classes").zipTo(Paths.get("mylib.jar"));

```

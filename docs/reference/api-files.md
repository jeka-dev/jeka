# Files API

File manipulation is a key aspect of software development.  
Jeka extends the *java.nio.file* API by introducing additional concepts to offer a powerful and fluent API for performing common tasks with minimal effort.  

The *file-api* classes are located in the `dev.jeka.core.api.file` package. Visit the [Javadoc](https://jeka-dev.github.io/jeka/javadoc/dev/jeka/core/api/file/package-summary.html).

## `JkPathFile` 
A simple wrapper for files (not folders). It provides copying, interpolation, checksum, deletion and creation methods.

```java title="Example"
JkPathFile.of("my-new-file.txt")
    .fetchContentFrom("https://www.bgreco.net/fortune.txt")
    .copyToDir(myFolder);
```

## `JkPathTree`
An Immutable root folder along a `PathMatcher` providing operations to copy, navigate, zip, iterate, or watch.
This is a central class in Jeka API.

```java title="Examples"

// copies all non java source files to another directory preserving structure
JkPathTree.of("src").andMatching(false, "**/*.java").copyTo("build/classes");

// One liner to zip an entire directory
JkPathTree.of("build/classes").zipTo(Paths.get("mylib.jar"));

// Traverse the tree in breath first (opposite to deep-first) 
JkPathTree.of("build/classes").streamBreathFirst().forEach(file -> ...);

// Compute MD5
String md5 = JkPathTree.of("src/main/java").checksum();
```


## `JkPathSequence` 
An Immutable sequence of `java.nio.file.Path` providing methods for filtering or appending.
```java title="Example"
URL[] urls = JkPathSequence.of(paths)
                .and(embeddedLibs())
                .toUrls();
```

## `JkPathMatcher`
An immutable `java.nio.file.PathMatcher` based on `java.nio.file` glob pattern or regerxp.
  Used by `JkPathTree` to filter in/out files according name patterns.



## `JkZipTree` 
Same as `JkPathTree` but using a zip file instead of a directory. It allows you to manipulate a zip file
  as a regular folder. 

## `JkPathTreeSet` 
An Immutable set of `JkPathTree`. Helpful to define a set of sources/resources and create jar/zip files.

## `JkResourceProcessor` 
A mutable processor for copying a set of files, preserving the structure and
  replacing some texts with others. Typically used for replacing tokens `${server.ip}` with an actual values.


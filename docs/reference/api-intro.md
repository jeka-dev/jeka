# Build Library

JeKa comes with a set of classes designed to help you programmatically accomplish build tasks.  
These classes are used to implement JeKa's engine and its bundled KBeans.  
You may find these classes useful for your own needs, as they cover the following concerns:

- [File manipulation](api-files.md).
- Java specific actions (compilation, classloading, jar creation, ...). Visit [Javadoc](https://jeka-dev.github.io/jeka/javadoc/dev/jeka/core/api/java/package-frame.html).
- Java specific actions (compilation, module). Visit [source code](https://github.com/jeka-dev/jeka/tree/master/dev.jeka.core/src/main/java/dev/jeka/core/api/kotlin).
- Cryptography signing. Visit [source code](https://github.com/jeka-dev/jeka/tree/master/dev.jeka.core/src/main/java/dev/jeka/core/api/crypto).
- [Dependency management](api-dependency-management.md).
- [Project Building](api-project.md)
- Testing (run tests on junit-jupiter engine). Visit [Javadoc](https://jeka-dev.github.io/jeka/javadoc/dev/jeka/core/api/testing/package-frame.html).
- Running external commands. Visit Visit [Javadoc](https://jeka-dev.github.io/jeka/javadoc/dev/jeka/core/api/system/JkProcess.html).
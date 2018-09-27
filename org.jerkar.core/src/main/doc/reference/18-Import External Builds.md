## Import External Build

There is many way in Jerkar to perform multi-project build. One of is to import some builds into another.

### Principle

A _JkBuild_ class can import JkBuild instances from other projects. 

The current _build classpath_ is augmented with the _build classpath_ of imported projects.

Imported builds are not aware they are imported. In fact any build can imported. The relation is uni-directional.

### Declare Build Import

As the external project _build classpath_ is also imported, you can use the imported build class into your build class code.

This is done using `@JkImportBuild` annotation as shown below :  

```Java
public class MyBuild extends JkBuild {
    
    @JkImportBuild("../otherProject")   // Import a sibling project
    private BarBuild anImportedBuild;   // This is a build class from the sibling project

    public void doSomesthing() {
       anImportedBuild.doBar();   // use the build class defined in ../otherProject
       ...
```

Builds are imported transitively, this means that, in above example, if `BarBuild` imports in turn an other project, this 
last will be also imported. 

### Option Propagation

Options mentioned in command line are propagated to the imported builds. 

So for example you execute `jerkar java#pack -java#tests.fork`, the main build and all imported builds will run tests in a forked process.

### Method propagation

Methods mentioned in the command line are not automatically propagated to imported builds. Executing `jerkar clean` will 
only clean the current build project.

To propagate method call to every imported build, method name should be prefixed with a '*'. Executing `jerkar clean*` will 
invoke 'clean' method on the current _build class_ along along all imported build classes.

### Access Imported Builds Programmatically

You can access to the list of import build within a JkBuild instance using `JkBuild#ImportedBuilds` methods as show below :

```
public void doForAll() {
        this.importedBuilds().all().forEach(JkBuild::clean);
        this.importedBuilds().allOf(JkJavaProjectBuild.class).forEach(build -> build.java().pack());
    }
```


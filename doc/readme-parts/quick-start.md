# Quick start

1. Add the org.jerkar.core-fat.jar (found in the distrib) in your IDE build-path. This jar includes Jerkar core along plugins classes
2. Create a `build/spec` folder at the base of your project and make it a source folder in your IDE. In Jerkar, all related build stuff (build definition, local 3rd party libs, produced artifacts,...) lies under *build* directory
3. Write the build class extending JkJavaBuild in this directory (in whatever package)
4. If your project respect convention, do not need managed dependencies and don't do 'special' thing, you don't even need 2) and 3) points
5. Launch the `org.jerkar.Main` class in your IDE or type `jerkar` in the command line (with the root of your project as working directory)

This will launch the `doDefault` method defined in your build class. Note that this method is declared in the `JkJavaBuild` and invoke in sequence clean, compile, unitTest and pack methods.

If you want to launch several methods of your build, type `jerkar doSomething doSomethingElse`. Jerkar recognizes any public zero-argument method returning `void` as build method.
Type `jerkar help` to get all the build methods provided by your build class. 
  

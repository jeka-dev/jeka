Jeka follows a specific process before invoking _KBean methods_ :

1. Parse command line and set system properties.
2. Set working dir as _[project dir]_
3. Augment the current classloader with jar files found in _[project dir]/jeka/boot_
4. Parses source files contained in _project dir/jeka/def_
      1. Detect `@JkInjectProject` annotations. For each : 
         1. Set _[project dir]_ to value declared in annotation
         2. Process steps _3, 4, 5, 6_ for the injected project
      2. Augment the current classloader with dependencies declared in source files
5. Compile files in _[project dir]/def/jeka_ and augment current classloader with compiled files
6. If compilation fail and command-line option `-dci` is present, ignore it.
7. Scan classloader classpath to find _KBeans_ and associate each _KBean_ referenced in
   command line
8. Identify the default _KBean_ if any. The default _KBean_ is the first class found 
   in _[project dir]/jeka/def_ extending `JkBean`.
9. Instantiate the default _KBean_. Instantiate means :
      1. Invoke _no-arg_ constructor
      2. Inject _KBean properties_ mentioned in command line
10. Instantiate other _KBean_, if not yet done by order they appear in command line.

Once _KBeans_ have been instantiated, _KBean methods_ are executed in order they appear
in the command line.

!!! info
    There is only one _KBean_ instance by _KBean_ class and by project.

!!! info
    `JkRuntime` is the _KBean_ container for a given project. Each _KBean_ instance 
     belongs to a `JkRuntime`. From `JkRuntime`, you can access to other _KBeans_ 
     from same or other projects.

Jeka follows a specific process before invoking _KBean methods_ :

1. Parse command line and set arguments specified as *-Dxxx=yyy* as system properties.
2. Set _[project dir]_ = _current working dir_
3. Augment the current classloader with jar files found in _[project dir]/jeka/boot_ 
4. Augment the current classloader with resolved coordinates mentionned in command line as *@group:name:version*
5. Parse source files contained in _project dir/jeka/def_
      1. Detect `@JkInjectProject` annotations. For each : 
         1. Set _[project dir]_ to value declared in annotation
         2. Process steps _3, 4, 5, 6_ for the injected project
      2. Augment the current classloader with dependencies declared in source files
6. Compile files in _[project dir]/def/jeka_ and augment current classloader with compiled files
7. If the compilation fails and the command-line option `-dci` is present, ignore it.
8. Scan classloader classpath to find _KBeans_ and associate each _KBean_ referenced in the
   command line.
8. Identify the default _KBean_ if available. The default _KBean_ is the first class found 
   in _[project dir]/jeka/def_ extending `JkBean`.
9. Instantiate the default _KBean_. Instantiate means :
      1. Invoke _no-arg_ constructor
      2. Inject _KBean properties_ mentioned in command line
10. Instantiate other _KBean_, if not yet done by order they appear in the command line.

Once _KBeans_ have been instantiated, _KBean methods_ are executed in the order in which they appear
in the command line.

!!! info
    There is only one _KBean_ instance by _KBean_ class and by project.

!!! info
    `JkRuntime` is the _KBean_ container for a given project. Each _KBean_ instance 
     belongs to a `JkRuntime`. From `JkRuntime`, you can access other _KBeans_ 
     from the same or other projects.

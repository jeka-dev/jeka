# Reuse build logic (Work in progress)

Jeka offers flexible and powerful mechanisms for reusing build logic, such as : 

- Adding Java dependencies to *def* classpath.
- Importing build code from another module (see example [here](https://github.com/jerkar/working-examples/tree/master/springboot-multi-modules))
- Using *plugins*

## Add dependencies to *def* classpath

This simple mechanism is powerful. It lets *def* classes depend on other Java code just as it they would
for test/production code. This means that *def* classes can depend on :

- code lying in a different module from the same multi-module project
- compiled code lying somewhere on the filesystem
- jars located in a binary repository (i.e. Maven modules)

This tutorial will give some simple examples on how you can factor out parts of your builds. 


With Jeka you can reuse and share any build elements exactly as you would do for regular code.
You can reuse a piece of code within a multi-module project or export it on a repository to reuse it 
across projects.

These pieces of code can be Jeka plugins or simple classes to make your build code shorter.



## Create your own plugin

A plugin is a collection of commands and options (meaning public no-args methods and fields) that can be bound to any 
JkClass in order to augment it or modify its behavior. 

For common usage, you don't need to write your own plugin but you will probably use the ones that are bundled with 
Jeka. The simplest way to understand how it works, is to write your own.


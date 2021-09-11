# Reuse build logic (Work in progress)

Jeka offers flexible and powerful mechanisms to reuse build logic, as : 
- Adding Java dependencies to *def* classpath.
- Importing build code from another module (see example [here](https://github.com/jerkar/working-examples/tree/master/springboot-multi-modules))
- Using *plugins*

## Add dependencies to *def* classpath

This simple mechanism is powerful. It let your *def* classes depends on other Java code just as you would de
for your test/production code. It means that you can make your *def* classes depend on :
- code lying in a different module from the same multi-module project
- compiled code lying somewhere on your filesystem
- jars located in a binary repository (i.e. Maven modules)

This tutorial will give some simple examples on how you can factor out parts of your builds. 


With Jeka you can reuse and share any build elements exactly as you would do for regular code.
You can reuse piece of code within a multi-module project or export it on a repository to reuse it 
across projects.

These piece of code can be Jeka plugins or simple classes for make your build code shorter, tough you
can much smarter thing 




## Create your own plugin

A plugin is a collection of commands and options (meaning public no-args methods and fields) that can be bind to any 
JkClass in order to augment it or modify its behavior. 

For common usage, you don't need to write your own plugin but you will probably uses the ones that are bundled with 
Jeka. The simplest think to understand how it works, is to write your own one.


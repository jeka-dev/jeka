Libraries found here will be included in Jerkar classpath.
So for example, if you put Guava library here you can use the Guava library in any Jerkar build file. 
These libraries precede jerkar.jar in the classpath so it can redefine default implementation class.

It is meant to allow users extend Jerkar althrough it is not recommended as your builds may not be reproducible 
on another Jerkar installation.

 
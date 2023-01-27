# Jeka library/plugin for NodeJs

Plugin to integrate with NodeJs.

This plugin provides :
- a KBean to automatically download/install the specified version of NodeJs
- A utility class to invoke NodeJs tool in a platform independent manner.

To use in your Kbean, import it using `@JkImportClasspath("dev.jeka:nodejs-plugin")` 

Then you can define the NodeJs version to use and invoke *npm* and other commands from `NodeJsJkBean`.

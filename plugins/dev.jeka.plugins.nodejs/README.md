# NodeJs plugin for JeKa

Plugin to integrate with NodeJs.

This plugin provides :
- a KBean to automatically download/install the specified version of NodeJs
- A utility class to invoke NodeJs tool in a platform independent manner.

To use in your project, import it using `@JkInjectClasspath("dev.jeka:nodejs-plugin")` 

## Examples

Execute an *npm* or *npx* command :
```java
// '20.10.0' is the NodeJs version to use
// './js-project' is the working directory to execute nodeJs from
JkNodeJs.ofVersion("20.10.0").setWorkingDir("./js-project").exec("npm run build");
```

Configure a JVM project to include a ReactJs build. The bundled js will be copied to *resources/static* of the Jvm Project :
```java
// '20.10.0' is the NodeJs version to use
// 'project' is an instance of JkProject
// 'reactjs-client' is the path, relative to project root, of the reacjJs project
// 'build' is the path, relative to reactJs project root, of the directory containing the bundled js
// 'npx yarn install', 'npm run build' are the nodeJs commands to build the reactJs project
JkNodeJs.ofVersion("20.10.0").configure(project, "reactjs-client", "build",
                    "npx yarn install ", "npm run build");
```


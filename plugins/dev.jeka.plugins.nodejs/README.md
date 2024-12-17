# NodeJs Plugin for JeKa

This Plugin provides a [KBean](src/dev/jeka/plugins/nodejs/NodeJsKBean.java) to auto-configure projects to build nodeJs application and optionally 
embed it in the built Jar. The nodeJs apllication is built using the specified *NodeJs* version (automatically downloaded) 
and built using the specified *npm* commands.

This plugin is not meant to replace nodeJs developer tools, bu just a meant to build nodeJs app 
along Java build.

This plugin also provides a class to install and invoke NodeJs/npm/npx from Java.

**Command Line Documentation:** `jeka nodejs: --doc`

**KBean Source Code:** [Visit here](src/dev/jeka/plugins/nodejs/NodeJsKBean.java).

**Example:** [Springboot + Angular](https://github.com/jeka-dev/demo-project-springboot-angular/tree/master)

## Actions on Initialization

The plugin configures the `ProjectKBean` to include post actions in *compile* and *test* phases.

## Configuration

```properties
jeka.classpath.inject=dev.jeka:nodejs-plugin
@nodeJs=

# required configuration
@nodeJs.cmdLine=npm install, npm run build

# Optional 
@nodeJs.version=18.18.0
@nodeJs.autoConfigureProject=true
@nodeJs.distDir=dist/client/browser
@nodeJs.targetResourceDir=static
@nodeJs.testCmdLine=npm run test-headless
```

- **autoConfigureProject:** If false, the project won't be automatically configured.
- **appDir:** Instruct plugin where the nodeJs application to build is located *(default is 'app-js')*.
- **cmdLine:** Coma separated string mentioning the *npm* command to run to build the nodeJs app.
- **testCmdLine:** Coma separated string mentioning the *npm* command to run tests (optional).
- **distDir:** Instruct the plugin where to find the app built by npm instructions.
- **targetResourceDir:** Directory, relative to resources, where the *distDir* should be copied (optional).
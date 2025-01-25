# Node.js Plugin for JeKa

This KBean expects the project to include a folder with a Node.js application. Simply specify the Node.js version and the commands to build or test the application. The KBean guides the `project KBean` to build and test it.
Node.js is automatically downloaded and installed on first use, so no manual setup is required.

|Field  |Description  |Type  |
|-------|-------------|------|
|version |The version of NodeJs to use |String |
|cmdLine |Command line to run when `exec` is called, e.g., 'npx cowsay'. |String |
|buildCmd |Comma separated, command lines to execute for building js application or in conjunction with #exec method. This can be similar to something like 'npx ..., npm ...' |String |
|testCmd |Comma separated, command lines to execute for testing when 'autoConfigureProject'=true |String |
|appDir |Path of js project root. It is expected to be relative to the base directory. |String |
|buildDir |Path to the built app (usually contains an index.html file). Relative to the JS app directory. |String |
|targetResourceDir |If set, copies the client build output to this directory, relative to the generated class directory (e.g., 'static'). |String |
|configureProject |If true, the project wrapped by ProjectKBean will be configured automatically to build the nodeJs project. |boolean |


|KBean Initialisation  |
|--------|
|Optionally configures the `project` KBean in order it includes building of a JS application.<br/> |


|Method  |Description  |
|--------|-------------|
|build |Builds the JS project by running the specified build commands. This usually generates packaged JS resources in the project's build directory. |
|clean |Deletes the build directory. |
|exec |Executes the nodeJs command line mentioned in `cmdLine` field. |
|info |Displays configuration info. |
|pack |Packs the JS project as specified in project configuration. It generally leads to copy the build dir into the static resource dir of the webapp. |
|test |Runs the test commands configured for the JS project. |


This plugin is not intended to replace Node.js developer tools but is designed to help build Node.js applications alongside Java-based projects.

Additionally, this plugin provides a utility class to install and invoke Node.js, npm, and npx directly from Java code.

Resources:
- Command-line Documentation: `jeka nodeJs: --doc`
- KBean Source Code: [View here](src/dev/jeka/plugins/nodejs/NodeJsKBean.java).
- Example Project: [Spring Boot + Angular](https://github.com/jeka-dev/demo-project-springboot-angular/tree/master)

## Initialization

During initialization, the plugin configures `ProjectKBean` to include post-actions in the *compile* and *test* phases.

## Configuration

```properties
jeka.classpath.inject=dev.jeka:nodejs-plugin
@nodeJs=

# Required configuration
@nodeJs.cmdLine=npm install, npm run build

# Optional configurations
@nodeJs.version=22.10.0
@nodeJs.configureProject=true
@nodeJs.buildDir=build
@nodeJs.targetResourceDir=static
@nodeJs.testCmdLine=npm run test-headless
```

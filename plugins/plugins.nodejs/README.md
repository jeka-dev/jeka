# Node.js Plugin for JeKa

Handles building a Node.js project.


This KBean expects the project to include a folder with a Node.js application. Simply specify the Node.js version and the commands to build or test the application. The KBean guides the `project KBean` to build and test it.
Node.js is automatically downloaded and installed on first use, so no manual setup is required.


|KBean Initialisation  |
|--------|
|Optionally configures the `project` KBean in order it includes building of a JS application.<br/>. |


**This KBean post-initializes the following KBeans:**

|Post-initialised KBean   |Description  |
|-------|-------------|
|ProjectKBean |Prepends nodeJs build to `pack` actions in order to embed js build in JAR, and appends js tests to `test` actions. |


**This KBean exposes the following fields:**

|Field  |Description  |
|-------|-------------|
|version [String] |The version of NodeJs to use. |
|cmdLine [String] |Command-line to run when `exec` is called, e.g., 'npx cowsay'. |
|buildCmd [String] |Comma separated, command-lines to execute for building js application or in conjunction with #exec method. This can be similar to something like 'npx ..., npm ...'. |
|testCmd [String] |Comma separated, command-lines to execute for testing when 'autoConfigureProject'=true. |
|appDir [String] |Path of js project root. It is expected to be relative to the base directory. |
|buildDir [String] |Path to the built app (usually contains an index.html file). Relative to the JS app directory. |
|targetResourceDir [String] |If set, copies the client build output to this directory, relative to the generated class directory (e.g., 'static'). |
|configureProject [boolean] |If true, the project wrapped by ProjectKBean will be configured automatically to build the nodeJs project. |


**This KBean exposes the following methods:**

|Method  |Description  |
|--------|-------------|
|build |Builds the JS project by running the specified build commands. This usually generates packaged JS resources in the project's build directory. |
|clean |Deletes the build directory. |
|exec |Executes the nodeJs command line mentioned in `cmdLine` field. |
|info |Displays configuration info. |
|pack |Packs the JS project as specified in project configuration. It usually leads to copy the build dir into the static resource dir of the webapp. |
|test |Runs the test commands configured for the JS project. |


Resources:

- KBean Source Code: [View here](src/dev/jeka/plugins/nodejs/NodeJsKBean.java).
- Example Project: [Spring Boot + Angular](https://github.com/jeka-dev/demo-project-springboot-angular/tree/master)

## Configuration Example

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

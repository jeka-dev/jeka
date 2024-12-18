# Node.js Plugin for JeKa

This plugin provides a [KBean](src/dev/jeka/plugins/nodejs/NodeJsKBean.java) to enable auto-configuration of projects for building Node.js applications. It optionally embeds the built Node.js application into the final JAR. The Node.js application is built using the specified *Node.js* version (automatically downloaded) and executed with the configured *npm* commands.

This plugin is not intended to replace Node.js developer tools but is designed to help build Node.js applications alongside Java-based projects.

Additionally, this plugin provides a utility class to install and invoke Node.js, npm, and npx directly from Java code.

Resources:
- Command-line Documentation: `jeka nodejs: --doc`
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
@nodeJs.version=18.18.0
@nodeJs.configureProject=true
@nodeJs.distDir=dist/client/browser
@nodeJs.targetResourceDir=static
@nodeJs.testCmdLine=npm run test-headless
```

### Configuration Properties

- **`configureProject`**: If `false`, automatic project configuration will not be performed.
- **`appDir`**: Specifies the location of the Node.js source application (default: `app-js`).
- **`cmdLine`**: Comma-separated string of *npm* or *npx* commands that will build the Node.js application (e.g., `npm install, npm run build`).
- **`testCmdLine`**: Comma-separated string of *npm* or  *npx* commands to run tests (optional).
- **`distDir`**: Specifies the directory where the built application exists after running the npm commands.
- **`targetResourceDir`**: Specifies the directory (relative to the resources folder) where the `distDir` files should be copied (optional).
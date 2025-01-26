/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.jeka.plugins.nodejs;

import dev.jeka.core.api.depmanagement.JkDepSuggest;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.text.Jk2ColumnsText;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkException;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@JkDoc("Handles building a Node.js project.\n\n" +
        "This KBean expects the project to include a folder with a Node.js application. " +
        "Simply specify the Node.js version and the commands to build or test the application. " +
        "The KBean guides the `project KBean` to build and test it.\n" +
        "Node.js is automatically downloaded and installed on first use, so no manual setup is required.")
public class NodeJsKBean extends KBean {

    public static final String CLEAN_ACTION = "nodejs-clean";

    @JkDoc("The version of NodeJs to use")
    @JkDepSuggest(versionOnly = true, hint = "22.11.0,20.9.0,18.19.0,16.20.2")
    public String version = JkNodeJs.DEFAULT_NODE_VERSION;

    @JkDoc("Command line to run when `exec` is called, e.g., 'npx cowsay'.")
    public String cmdLine;

    @JkDoc("Comma separated, command lines to execute for building js application or in conjunction with #exec method. " +
            "This can be similar to something like 'npx ..., npm ...'")
    public String buildCmd;

    @JkDoc("Comma separated, command lines to execute for testing when 'autoConfigureProject'=true")
    public String testCmd;

    @JkDoc("Path of js project root. It is expected to be relative to the base directory.")
    public String appDir = "app-js";

    @JkDoc("Path to the built app (usually contains an index.html file). Relative to the JS app directory.")
    public String buildDir = "build";

    @JkDoc("If set, copies the client build output to this directory, relative to the generated class directory (e.g., 'static').")
    public String targetResourceDir;

    @JkDoc("If true, the project wrapped by ProjectKBean will be configured automatically to build the nodeJs project.")
    public boolean configureProject = false;

    private JkNodeJsProject nodeJsProject;

    @JkDoc("Optionally configures the `project` KBean in order it includes building of a JS application.\n")
    @Override
    protected void init() {
        if (configureProject) {
            configureProject();
        }
        getRunbase().registerCLeanAction(CLEAN_ACTION, this::cleanBuildDir);
    }

    @JkDoc("Builds the JS project by running the specified build commands. " +
            "This usually generates packaged JS resources in the project's build directory.")
    public void build() {
        if (nodeJsProject == null) {
            throw new JkException("The project has been configured to build with NodeJs.");
        }
        nodeJsProject.build();
        JkLog.info("Build generated in %s dir.", getBaseDir().relativize(nodeJsProject.getBuildDir()));
    }

    @JkDoc("Runs the test commands configured for the JS project.")
    public void test() {
        if (nodeJsProject == null) {
            throw new JkException("The project has been configured to build with NodeJs.");
        }
        nodeJsProject.test();
    }

    @JkDoc("Packs the JS project as specified in project configuration. " +
            "It usually leads to copy the build dir into the static resource dir of the webapp.")
    public void pack() {
        if (nodeJsProject == null) {
            throw new JkException("The project has been configured to build with NodeJs.");
        }
        nodeJsProject.pack();
    }

    @JkDoc("Executes the nodeJs command line mentioned in `cmdLine` field.")
    public void exec() {
        JkNodeJs.ofVersion(version).exec(cmdLine);
    }

    @JkDoc("Displays configuration info.")
    public void info() {
        String version = this.nodeJsProject == null ? this.version : nodeJsProject.getNodeJs().getVersion();
        Jk2ColumnsText text =Jk2ColumnsText.of(18, 80)
                .add("NodeJs Version", version);
        if (nodeJsProject != null) {
            text    .add("Build Commands", String.join(", ", nodeJsProject.getBuildCommands()))
                    .add("Test Commands", String.join(", ", nodeJsProject.getTestCommands()))
                    .add("Deploy action?", Boolean.toString(nodeJsProject.getPackAction() != null));
        }
        System.out.println(text);
    }

    @JkDoc("Deletes the build directory.")
    public void clean() {
        cleanBuildDir();
        JkLog.info("Build dir %s deleted.", getBaseDir().relativize(nodeJsProject.getBuildDir()));
    }

    /**
     * Configures the Node.js project by setting up the Node.js version,
     * project paths, build commands, and test commands. Optionally sets
     * the resource packing action if a target resource directory is specified.
     *
     * @return the current instance of NodeJsKBean after configuring the project.
     */
    public JkNodeJsProject configureProject() {
        JkProject project = load(ProjectKBean.class).project;
        JkNodeJs nodeJs = JkNodeJs.ofVersion(this.version);
        Path baseJsDirPath = getBasePath(appDir);
        this.nodeJsProject = JkNodeJsProject.of(nodeJs, baseJsDirPath, buildDir)
                .setBuildCommands(commandLines(buildCmd))
                .setTestCommands(commandLines(testCmd));
        if (!JkUtilsString.isBlank(targetResourceDir)) {
            this.nodeJsProject.setCopyToResourcesPackAction(project, targetResourceDir);
        }
        this.nodeJsProject.registerIn(project);
        return this.nodeJsProject;
    }

    private void cleanBuildDir() {
        if (nodeJsProject != null) {
            JkUtilsPath.deleteQuietly(nodeJsProject.getBuildDir(), false);
        }
    }

   private static String[] commandLines(String cmd) {
        if (cmd == null) {
            return new String[0];
        }
        return Stream.of(cmd.split(","))
                .map(String::trim)
                .toArray(String[]::new);
   }

}

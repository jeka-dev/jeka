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
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@JkDoc("Auto-configure projects with nodeJs client.")
public class NodeJsKBean extends KBean {

    @JkDoc("The version of NodeJs to use")
    @JkDepSuggest(versionOnly = true, hint = "20.10.0,18.19.0,16.20.2")
    public String version = JkNodeJs.DEFAULT_NODE_VERSION;

    @JkDoc("Comma separated, command lines to execute for building js application or in conjunction with #exec method. " +
            "This can be similar to something like 'npx ..., npm ...'")
    public String cmdLine;

    @JkDoc("Comma separated, command lines to execute for testing when 'autoConfigureProject'=true")
    public String testCmdLine;

    @JkDoc("Path of js project root. It is expected to be relative to the base directory.")
    public String appDir = "app-js";

    @JkDoc("Path of the built application (Generally containing an index.html file). " +
            "It is expected to be relative to the js app dir.")
    public String distDir = "build";

    @JkDoc("If not empty, the result of client build will be copied to this dir relative to class dir (e.g. 'static')")
    public String copyToDir;

    @JkDoc("If true, the project wrapped by ProjectKBean will be configured automatically to build the nodeJs project.")
    public boolean autoConfigureProject = false;

    @JkDoc("Execute npm using the command line specified in 'cmdLine' property.")
    public void exec() {
        commandLines(cmdLine).forEach(getJkNodeJs()::exec);
    }

    @JkDoc("Execute the command line specified by 'testCmdLine'")
    public void execTest() {
        commandLines(testCmdLine).forEach(getJkNodeJs()::exec);
    }

    @Override
    protected void init() {
        if (autoConfigureProject) {
            JkProject project = load(ProjectKBean.class).project;
            JkNodeJs.ofVersion(this.version)
                    .configure(project, appDir, distDir, copyToDir, commandLines(cmdLine),
                            commandLines(testCmdLine));
        }
    }

    /**
     * Returns the working directory for the NodeJsKBean. If the specified appDir does not exist,
     * the base directory is returned.
     *
     * @return The working directory as a Path object.
     */
    public Path getWorkingDir() {
        if (JkUtilsString.isBlank(appDir)) {
            return getBaseDir();
        }
        Path result = getBaseDir().resolve(appDir);
        if (!Files.exists(result)) {
            JkLog.info("Directory not found %s, use base dir as working dir.", result);
            return getBaseDir();
        }
        return result;
    }

    private JkNodeJs getJkNodeJs() {

        return JkNodeJs.ofVersion(version).setWorkingDir(getWorkingDir());
    }



   private static List<String> commandLines(String cmd) {
        if (cmd == null) {
            return Collections.emptyList();
        }
        return Stream.of(cmd.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
   }


}

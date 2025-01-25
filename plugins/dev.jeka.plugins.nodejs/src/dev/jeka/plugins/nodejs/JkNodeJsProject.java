/*
 * Copyright 2014-2025  the original author or authors.
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

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsTime;
import dev.jeka.core.tool.JkException;

import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class JkNodeJsProject {

    private static final String BUILD_TASK_NAME = "build-js";

    private static final String TEST_TASK_NAME = "test-js";

    private static final String PACK_TASK_NAME = "pack-js";

    private final JkNodeJs nodeJs;;

    private Path baseJsDir;

    // relative to baseJsDir
    private String buildDir;

    private List<String> buildCommands = Collections.emptyList();

    private List<String> testCommands = Collections.emptyList();

    private Consumer<Path> packAction;


    private JkNodeJsProject(JkNodeJs nodeJs, Path baseJsDir, String buildDir, List<String> buildCommands) {
        this.nodeJs = nodeJs.setWorkingDir(baseJsDir);
        this.baseJsDir = baseJsDir;
        this.buildDir = buildDir;
        this.buildCommands = buildCommands;
    }

    public static JkNodeJsProject of(JkNodeJs nodeJs, Path baseJsDir, String buildDir, List<String> buildCommands) {
        return new JkNodeJsProject(nodeJs, baseJsDir, buildDir, buildCommands);
    }

    public static JkNodeJsProject of(JkNodeJs nodeJs, Path baseJsDir, String buildDir) {
        return new JkNodeJsProject(nodeJs, baseJsDir, buildDir, Collections.emptyList());
    }

    /**
     * Executes the build phase for the Node.js project using the specified build commands.
     *
     * @throws JkException if no build commands are defined or there is a misconfiguration.
     */
    public JkNodeJsProject build() {
        if (buildCommands.isEmpty()) {
            throw new JkException("No build command found for JS project %s. Add at least one to proceed.", baseJsDir);
        }
        if (!JkLog.isVerbose()) {
            JkLog.info("Building JS project with Node.js. This may take some time. Please be patient.");
            JkLog.info("Use the `--verbose` option to show progress. NodeJS build started at %s.",
                    JkUtilsTime.now("HH:mm:ss"));
        }
        buildCommands.forEach(nodeJs::exec);
        return this;
    }

    /**
     * Executes the test phase for the Node.js project.
     */
    public JkNodeJsProject test() {
        if (testCommands.isEmpty()) {
            JkLog.verbose("No test commands defined for JS project. No test to run.");
        }
        if (!JkLog.isVerbose()) {
            JkLog.info("Running JS project test with Node.js. This may take some time. Please be patient.");
            JkLog.info("Use the `--verbose` option to show progress. NodeJS build started at %s.",
                    JkUtilsTime.now("HH:mm:ss"));
        }
        testCommands.forEach(nodeJs::exec);
        return this;
    }

    /**
     * Executes the packaging phase for the Node.js project. If a packaging action
     * has been defined, it will be applied to the build directory of the project.
     */
    public JkNodeJsProject pack() {
        if (packAction == null) {
            JkLog.verbose("No packaging action defined for node.js project.");
            return this;
        }
        JkLog.verbose("Packaging NodeJs build.");
        packAction.accept(getBuildDir());
        return this;
    }

    /**
     * Sets the build commands to be executed during the build phase
     * of the Node.js project. The provided commands will replace
     * any previously set build commands.
     */
    public JkNodeJsProject setBuildCommands(String... buildCommands) {
        this.buildCommands = Arrays.asList(buildCommands);
        return this;
    }

    /**
     * Sets the test commands to be executed during the test phase
     * of the Node.js project. The provided commands will replace
     * any previously set test commands.
     */
    public JkNodeJsProject setTestCommands(String... testCommands) {
        this.testCommands = Arrays.asList(testCommands);
        return this;
    }

    /**
     * Sets the packaging action for the Node.js project. This defines
     * what happens during the packaging phase, working on the build
     * directory of the project.
     *
     * @param packAction a Consumer<Path> that specifies the packaging logic
     *                   for the build directory
     */
    public JkNodeJsProject setPackAction(Consumer<Path> packAction) {
        this.packAction = packAction;
        return this;
    }

    /**
     * Sets an action to copy the build directory contents of the Node.js project
     * to the specified relative path within the given JkProject's class output directory.
     *
     * @param project the JkProject instance whose class output directory is the target for the copy
     * @param relPath the relative path within the class output directory where the build contents will be copied
     * @return this JkNodeJsProject instance for further configurations
     */
    public JkNodeJsProject setCopyToResourcesPackAction(JkProject project, String relPath) {
        Path target = project.compilation.layout.resolveClassDir().resolve(relPath);
        JkPathTree.of(getBuildDir()).copyTo(target, StandardCopyOption.REPLACE_EXISTING);
        JkLog.info("Build dir copied to %s", target);
        return this;
    }

    /**
     * Registers this Node.js project within the provided JkProject instance,
     * integrating build, test, and packaging tasks.
     *
     * @param project the JkProject instance to register this Node.js project in
     * @return this JkNodeJsProject instance for further configurations
     */
    public JkNodeJsProject registerIn(JkProject project) {
        project.compilation.postCompileActions.append(BUILD_TASK_NAME, this::build);
        project.testing.postActions.append(TEST_TASK_NAME, this::test);
        project.packActions.insertBefore(PACK_TASK_NAME, JkProject.CREATE_JAR_ACTION, this::pack);
        return this;
    }

    /**
     * Retrieves the base directory for the Node.js project.
     */
    public Path getBaseJsDir() {
        return baseJsDir;
    }

    public JkNodeJsProject setBaseJsDir(Path baseJsDir) {
        this.baseJsDir = baseJsDir;
        this.nodeJs.setWorkingDir(baseJsDir);
        return this;
    }

    public JkNodeJsProject setBuildDir(String buildDir) {
        this.buildDir = buildDir;
        return this;
    }

    /**
     * Retrieves the build directory for the Node.js project.
     */
    public Path getBuildDir() {
        return baseJsDir.resolve(buildDir);
    }
}

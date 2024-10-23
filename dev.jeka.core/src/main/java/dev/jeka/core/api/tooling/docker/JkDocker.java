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

package dev.jeka.core.api.tooling.docker;

import dev.jeka.core.api.depmanagement.JkCoordinate;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcResult;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.utils.JkUtilsAssert;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class providing utility methods for executing Docker commands.
 */
public class JkDocker {

    /**
     * Executes the specified Docker command with the given parameters and returns the result.
     *
     * @param dockerCommand The Docker command to execute as 'run', 'tag', 'version', ....
     * @param params        The parameters to pass to the Docker command.
     * @return The result of the command execution.
     */
    public static JkProcResult exec(String dockerCommand, String ...params) {
        return prepareExec(dockerCommand, params).exec();
    }

    /**
     * Executes the specified Docker command with the given parameters expressed with
     * a space separated string of arguments.
     *
     * @param dockerCommand The space separated String representing the arguments.
     * @param cmdLineArgs       The parameters to pass to the Docker command as a space separated string (e.g. "-X -e run").
     * @see JkDocker#exec(String, String...)
     * @see JkProcess#addParamsAsCmdLine(String, Object...) (String)
     */
    public static JkProcResult execCmdLine(String dockerCommand, String cmdLineArgs) {
        return prepareExec(dockerCommand).addParamsAsCmdLine(cmdLineArgs).exec();
    }

    /**
     * Prepares a JkProcess object to execute a Docker command with the given parameters.
     */
    public static JkProcess prepareExec(String dockerCommand, String ...params) {
        return JkProcess.of("docker")
                .addParams(dockerCommand)
                .addParams(params)
                .setLogCommand(true)
                .setInheritIO(true)
                .setDestroyAtJvmShutdown(true)
                .setFailOnError(true);  // By default, it is wise to fail when an error occurs
    }

    /**
     * Checks if Docker is present on the system.
     */
    public static boolean isPresent() {
        try {
            return prepareExec("version")
                    .setLogCommand(JkLog.isVerbose())
                    .setInheritIO(false)
                    .setLogWithJekaDecorator(false)
                    .setFailOnError(false)
                    .exec()
                        .hasSucceed();
        } catch (UncheckedIOException e) {
            return false;
        }
    }

    public static void assertPresent() {
        JkUtilsAssert.state(isPresent(), "Operation halted. Docker client unresponsive. Is Docker daemon running?");
    }

    /**
     * Prepares a JkProcess object to execute Jeka in a Docker container.
     */
    public static Set<String> getImageNames() {
        List<String> rawResult = prepareExec("images", "--format", "{{.Repository}}:{{.Tag}}")
                .setCollectStdout(true)
                .setInheritIO(false)
                .exec().getStdoutAsMultiline();
        return rawResult.stream()
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    /**
     * Prepares a JkProcess object to execute Jeka in a Docker container.
     * This can be useful for creating native executable runnable in a container.
     * <p>
     * The latest <i>jekadev/jeka</i> image will be used to execute <i>jeka</i>.<br/>
     * The container will be map volumes to execute Jeka in the working directory, and
     * to fetch cache in <i>$HOME/.jeka/cache4c:/cache</i> dir.<br/>
     * You can force a specific version of Jeka by adding <code>-Djeka.version=xxx</code> to <code>jekaCommandArgs</code>
     *
     * @param jekaCommandArgs The argument to pass to JeKa
     */
    public static JkProcess prepareExecJeka(Path workingDir, String... jekaCommandArgs) {
        JkProcess process =  JkDocker.prepareExec("run" ,
                "-v", JkLocator.getCacheDir().getParent().resolve("cache4c").normalize() + ":/cache",
                "-v", workingDir.toAbsolutePath() + ":/workdir",
                "-t",
                "jekadev/jeka"
                );
        process.addParams(jekaCommandArgs);
        return process;
    }

    public static JkProcess prepareExecJeka(String... jekaCommandArgs) {
        return prepareExecJeka(Paths.get(""), jekaCommandArgs);
    }

    /**
     * Executes Jeka in a docker container.
     *
     * @see JkDocker#prepareExecJeka(Path, String...)
     */
    public static JkProcResult execJeka(String... jekaCommandArgs) {
        return prepareExecJeka(Paths.get(""), jekaCommandArgs).exec();
    }

}

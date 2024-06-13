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

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcResult;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.utils.JkUtilsAssert;

import java.io.IOException;
import java.io.UncheckedIOException;
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
     * Retrieves the names of all images present in the Docker registry.
     * The image are formatted as `repository:tag'. The tag is always present, it values 'latest' by default.
     *
     * @return A set of strings representing the names of the images in the repository.
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

}

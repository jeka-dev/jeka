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

import dev.jeka.core.api.system.JkAbstractProcess;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkProcResult;
import dev.jeka.core.api.utils.JkUtilsAssert;

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class providing utility methods for executing Docker commands.
 */
public class JkDocker extends JkAbstractProcess<JkDocker> {

    private JkDocker() {
        this.addParams("docker");
    }

    private JkDocker(JkDocker other) {
        super(other);
    }

    public static JkDocker of() {
        return new JkDocker();
    }

    @Override
    protected JkDocker copy() {
        return new JkDocker(this);
    }

    /**
     * Login to a Docker registry using the specified credentials.
     *
     * @param registry The URL or name of the Docker registry to log in to, i.e. $ACR_NAME.azurecr.io
     * @param username The username for authentication with the registry.
     * @param password The password for authentication with the registry.
     */
    public JkDocker login(String registry, String username, String password) {
        return execCmd("login", registry, "-u", username, "-p", password);
    }

    public JkDocker loginDockerHub(String username, String password) {
        return login("docker.io", username, password);
    }

    /**
     * Checks if Docker is present on the system.
     */
    public boolean isPresent() {
        try {
            return copy().addParams("version").exec().hasSucceed();
        } catch (UncheckedIOException e) {
            return false;
        }
    }

    /**
     * Asserts that Docker is present and responsive on the system.
     *
     * @throws IllegalStateException if the Docker daemon is not running or unresponsive.
     */
    public JkDocker assertPresent() {
        JkUtilsAssert.state(isPresent(), "Operation halted. Docker client unresponsive. Is Docker daemon running?");
        return this;
    }

    /**
     * Retrieves the names of Docker images available locally. The image names
     * are formatted as "repository:tag".
     *
     * @return A set of strings representing the names of local Docker images, where each name is in the format "repository:tag".
     */
    public  Set<String> getImageNames() {
        List<String> rawResult = copy().addParams("images", "--format", "{{.Repository}}:{{.Tag}}")
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
    public JkAbstractProcess<?> prepareExecJekaInDocker(Path workingDir, String... jekaCommandArgs) {
        JkDocker process =  copy().addParams("run" ,
                "-v", JkLocator.getCacheDir().getParent().resolve("cache4c").normalize() + ":/cache",
                "-v", workingDir.toAbsolutePath() + ":/workdir",
                "-t",
                "jekadev/jeka"
                );
        process.addParams(jekaCommandArgs);
        return process;
    }

    public JkAbstractProcess<?> prepareExecJekaInDocker(String... jekaCommandArgs) {
        return prepareExecJekaInDocker(Paths.get(""), jekaCommandArgs);
    }

    /**
     * Executes Jeka in a docker container.
     *
     * @see JkDocker#prepareExecJekaInDocker(Path, String...)
     */
    public JkProcResult execJekaInDocker(String... jekaCommandArgs) {
        return prepareExecJekaInDocker(Paths.get(""), jekaCommandArgs).exec();
    }

}

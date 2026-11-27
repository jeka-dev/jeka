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
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcResult;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsString;

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
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
            return copy()
                    .addParams("version")
                    .setLogCommand(JkLog.isDebug())
                    .setLogWithJekaDecorator(JkLog.isDebug())
                    .setFailOnError(false)
                    .exec().hasSucceed();
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
    public List<String> getLocalImages() {
        List<String> rawResult = copy().addParams("images", "--format", "{{.Repository}}:{{.Tag}}")
                .setCollectStdout(true)
                .setInheritIO(false)
                .exec().getStdoutAsMultiline();
        return rawResult.stream()
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Returns <code>true</code> if an image with the specified name exists locally.
     *
     * @param imageName The image name, as "my-image" or "my-image:1.0". Without tag, "latest" is assumed.
     */
    public boolean hasLocalImage(String imageName) {
        JkUtilsAssert.argument(!JkUtilsString.isBlank(imageName), "Image name cannot be blank.");
        return copy().addParams("image", "inspect", "--format", "{{.Id}}", imageName)
                .setLogCommand(JkLog.isDebug())
                .setLogWithJekaDecorator(JkLog.isDebug())
                .setInheritIO(false)
                .setCollectStdout(true)
                .setCollectStderr(true)
                .setFailOnError(false)
                .exec().hasSucceed();
    }

    /**
     * Removes the specified image from the local Docker images. Does nothing if the image does not exist.
     * Fails if a container, even stopped, uses the image.
     *
     * @param imageName The image name, as "my-image" or "my-image:1.0". Without tag, "latest" is assumed.
     * @return <code>true</code> if the image has been removed, <code>false</code> if it did not exist.
     */
    public boolean removeImage(String imageName) {
        if (!hasLocalImage(imageName)) {
            return false;
        }
        copy().addParams("image", "rm", imageName)
                .setLogCommand(JkLog.isVerbose())
                .setLogWithJekaDecorator(JkLog.isVerbose())
                .setInheritIO(false)
                .setCollectStdout(true)
                .exec();
        JkLog.verbose("Docker image %s removed.", imageName);
        return true;
    }

    /**
     * Removes the local images whose name, formatted as "repository:tag", starts with the specified prefix.
     * This is convenient to clean images created by tests, when they share a dedicated prefix.
     *
     * @param prefix The prefix of the images to remove, as "my-test-". It cannot be blank.
     * @return The names of the removed images.
     */
    public List<String> removeImagesStartingWith(String prefix) {
        JkUtilsAssert.argument(!JkUtilsString.isBlank(prefix), "Image name prefix cannot be blank.");
        List<String> imageNames = getLocalImages().stream()
                .filter(name -> name.startsWith(prefix))
                .collect(Collectors.toList());
        imageNames.forEach(this::removeImage);
        return imageNames;
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

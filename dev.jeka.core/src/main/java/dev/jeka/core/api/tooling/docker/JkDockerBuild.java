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

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.function.JkConsumers;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Docker Builder assistant for creating Docker images with 'nonroot' user.<br/>.
 * <p>
 * This clss allows to construct a Docker build context programmatically. <br/>
 * This means it can create a folder, containing the Dockerfile along the needed files to copy
 * in the image.
 * <p>
 *
 * The builder starts from a template DockerBuild that can be augmented using
 * methods and fields.
 *
 * <pre><code>
 * FROM ${BASE_IMAGE}
 *
 *     <------- setExposedPort()
 *
 * # create 'nonroot' user. Statements may vary according base image distro
 * # userId and groupId can be parametrized
 * # This section will be created only if #userId is not null
 * RUN addgroup --gid 1002 nonrootgroup && \
 *     adduser --uid 1001 --gid 1002 --disabled-password --gecos "" nonroot
 *
 *      <------ rootSteps
 *
 * This step will be created only if #userId is not null
 * USER nonroot
 *
 *      <------ nonRootSteps
 *
 *
 * </code></pre>
 */
public class JkDockerBuild {

    public static final String TEMURIN_ADD_USER_TEMPLATE = "RUN addgroup --gid ${GID} nonrootgroup && \\\n" +
            "    adduser --uid ${UID} --gid ${GID} --disabled-password --gecos \"\" nonroot";

    public static final String UBUNTU_ADD_USER_TEMPLATE = "RUN groupadd --gid ${GID} nonrootgroup && \\\n" +
            "    useradd --uid ${UID} --gid ${GID} --create-home nonroot && passwd -d nonroot";

    public static final String ALPINE_ADD_USER_TEMPLATE = "RUN addgroup --gid ${GID} nonrootgroup && \\\n" +
            "    adduser --uid ${UID} -g ${GID} --disabled-password nonroot\n";

    /**
     * Steps to be inserted in Dockerfile, prior 'nonroot' user switch.
     * These steps will be executed under 'root' user.
     */
    public final StepContainer rootSteps = new StepContainer();

    /**
     * Steps to be inserted in Dockerfile, after 'nonroot' user switch.
     * These steps will be executed under 'root' user.<p>
     * If the image is not defined to create a nonroot user, these steps will be executed under
     * root user, after the rootSteps.
     */
    public final StepContainer nonRootSteps = new StepContainer();

    private String baseImage = "ubuntu";

    private Integer userId = 1001;

    private Integer groupId;

    private String addUserTemplate;

    private final List<Integer> exposedPorts = new LinkedList<>();


    /**
     * Creates a {@link JkDockerBuild instannce.}
     */
    public static JkDockerBuild of() {
        return new JkDockerBuild();
    }

    protected JkDockerBuild() {
    }

    /**
     * Sets the base image name to use a s the base image in the docker file.
     */
    public final JkDockerBuild setBaseImage(String baseImage) {
        this.baseImage = baseImage;
        return this;
    }

    public String getBaseImage() {
        return baseImage;
    }

    /**
     * Set userId to be used as nonroot user.
     * @param userId The userId, or <code>null</code> to use the root user and skip nonroot user creation
     */
    public final JkDockerBuild setUserId(Integer userId) {
        this.userId = userId;
        return this;
    }

    public final JkDockerBuild setGroupId(int groupId) {
        this.groupId = groupId;
        return this;
    }

    /**
     * Sets the docker statement for adding a user.  "${UID}" and "$[GID}" will be
     * replaced by respectively the userId and the groupId set on this instance.
     */
    public final JkDockerBuild setAddUserTemplate(String addUserTemplate) {
        this.addUserTemplate = addUserTemplate;
        return this;
    }

    /**
     * Adds a port to be exposed by the container.
     */
    public final JkDockerBuild setExposedPorts(Integer ...ports) {
        this.exposedPorts.clear();
        this.exposedPorts.addAll(Arrays.asList(ports));
        return this;
    }

    /**
     * Returns an unmodifiable list of exposed ports in the Docker image.
     */
    public final List<Integer> getExposedPorts() {
        return Collections.unmodifiableList(exposedPorts);
    }

    /**
     * Builds the docker image with the specified image name.
     * @param contextDir the folder where the docker file will be generated, and from where will be resolve COPY paths.
     * @param imageName The name of the image to be build. It may contains or not a tag name.
     */
    public void buildImage(Path contextDir, String imageName) {
        JkDocker.assertPresent();
        this.generateContextDir(contextDir, false);
        JkDocker.prepareExec("build", "-t", imageName, contextDir.toString())
                .setInheritIO(true)
                .setLogCommand(true)
                .setLogWithJekaDecorator(true)
                .setInheritIO(false)
                .exec();

        String portMapping = portMappingArgs();
        JkLog.info("Run docker image       : docker run --rm %s%s", portMapping, imageName);
    }

    /**
     * Same as {@link #buildImage(Path, String)} but without needing passing a context directory.<p>
     * The context directory will be created on a random temp dir.
     * @return The path of the context dir.
     */
    public Path buildImage(String imageName) {
        Path contextDir = JkUtilsPath.createTempDirectory("jeka-docker-build-context");
        JkLog.verbose("Using context dir %s for building Docker image %s" , contextDir, imageName);
        buildImage(contextDir, imageName);
        return contextDir;
    }

    /**
     * Renders the effective Dockerfile
     */
    public final String render() {
        Path temp = JkUtilsPath.createTempDirectory("jk-dockerbuild-");
        String result = generateContextDir(temp, true);
        if (Files.exists(temp)) {
            JkPathTree.of(temp).deleteRoot();
        }
        return result;
    }

    /**
     * Returns a string representation of the command line arguments for port mapping in the Docker image.
     */
    public final String portMappingArgs() {
        String portMapping = "";
        if (!this.exposedPorts.isEmpty()) {
            portMapping = this.exposedPorts.stream()
                    .map(Object::toString)
                    .reduce("-p ", (init, port) -> init  + port + ":" + port + " ");
        }
        return portMapping;
    }

    /**
     * The specification of the 'nonroot' group id is optional.
     * If not mentioned explicitly, it values userId + 1
     * If userId values <code>null</code> and groupId, is not mentionned, this methods returns <code>null</code>
     */
    public final Integer getEffectiveGroupId() {
        if (this.groupId != null) {
            return groupId;
        }
        if (userId == null) {
            return null;
        }
        return groupId(userId);
    }

    /**
     * Returns <code>true</code> if a 'nonroot' is declaredd
     */
    public final boolean hasNonRootUser() {
        return userId != null;
    }

    /**
     * Transform a list of argument to a string as <code>["arg1", "arg2", ...]</code> suitable
     * to insert as ENTRYPOINT or CMD step arguments.
     */
    public static String toArrayQuotedString(List<String> args) {
        if (args == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        args.forEach(arg -> sb.append("\"").append(arg).append("\", "));
        if (!args.isEmpty()) {
            sb.setLength(sb.length() - 2);
        }
        sb.append("]");
        return sb.toString();
    }

    private String generateContextDir(Path contextDir, boolean dry) {

        // Handle exposed ports
        String exposedPortStatements = exposedPorts.isEmpty() ? "" :
                "EXPOSE " + exposedPorts.stream().map(i -> Integer.toString(i))
                        .collect(Collectors.joining(" ")) + "\n";

        String dockerBuildTemplate =
                "FROM ${baseImage}\n" +
                        exposedPortStatements +
                        userCreationStatement() +
                        this.rootSteps.process(contextDir, dry) +
                        "\n" +
                        switchUser() +
                        this.nonRootSteps.process(contextDir, dry);

        String dockerfileContent = dockerBuildTemplate.replace("${baseImage}", baseImage);
        if (!dry) {
            JkPathFile.of(contextDir.resolve("Dockerfile")).write(dockerfileContent);
        }
        return dockerfileContent;
    }

    private String userCreationStatement() {
        if (userId == null) {
            return "";
        }
        int gid = groupId(userId);
        String template = getAddUserTemplate() + "\n";
        return template.replace("${UID}", userId.toString()).replace("${GID}", Integer.toString(gid));
    }

    private String switchUser() {
        if (userId == null) {
            return "";
        }
        return "USER nonroot\n";
    }

    private static String toString(List<String> statements) {
        return statements.stream().reduce("", (init, step) -> init + step + "\n" );
    }

    private int groupId(int uid) {
        if (this.groupId != null) {
            return this.groupId;
        }
        return uid + 1;  // Some distrib as Alpine does not allow group creation having the same id than a user
    }

    private String getAddUserTemplate() {
        if (!JkUtilsString.isBlank(addUserTemplate)) {
            return addUserTemplate;
        }
        if (baseImage.contains("ubuntu")) {
            return UBUNTU_ADD_USER_TEMPLATE;
        } else if (baseImage.contains("temurin")) {
            return TEMURIN_ADD_USER_TEMPLATE;
        } else if (baseImage.contains("alpine")) {
            return ALPINE_ADD_USER_TEMPLATE;
        }
        return UBUNTU_ADD_USER_TEMPLATE;
    }

    public class StepContainer {

        private final JkConsumers<Context> operations = JkConsumers.of();

        /**
         * Appends the specified step in the Dockerfile.
         */
        public StepContainer add(String step) {
            operations.add(context -> context.steps.add(step));
            return this;
        }

        /**
         * Appends the specified operation to the build context
         */
        public StepContainer add(Consumer<Context> operation) {
            operations.add(operation);
            return this;
        }

        /**
         * Inserts a step in Dockerfile, just before the specified one.
         */
        public StepContainer insertBefore(String stepStartingWith, String ... stepsToInsert) {
            return add(context -> context.insertBefore(stepStartingWith, stepsToInsert));
        }

        /**
         * Convenient method to add a COPY step on a file/dir located outside of the building context.
         * The file will be copied within the Docker build context and a step will be added to Dockerfile.
         */
        public StepContainer addCopy(Path fileOrDir, String containerPath, boolean optional) {
            return add(context -> copyFileInContext(fileOrDir, containerPath, context, false, optional));
        }

        /**
         * Same as {@link #addCopy(Path, String, boolean)} with optional=false
         */
        public StepContainer addCopy(Path fileOrDir, String containerPath) {
            return addCopy(fileOrDir, containerPath, false);
        }

        /**
         * Adds a Dockerfile step that creates a directory, belonging to the 'nonroot' user if such a
         * user exists.
         */
        public StepContainer addNonRootMkdirs(String dirPath, String ...extraDirs) {
            return add(context -> {
                StringBuilder sb = new StringBuilder("RUN ");
                sb.append(mkdirInstrcution(dirPath));
                for (String extraDir : extraDirs) {
                    sb.append(" \\\n    && ");
                    sb.append(mkdirInstrcution(extraDir));
                }
                context.add(sb.toString());
            });
        }

        private String mkdirInstrcution(String dirPath) {
            if (userId == null) {
                return "mkdir -p " + dirPath;
            } else {
                return String.format("mkdir -p %s && chown -R ${uid}:${gid} %s", dirPath, dirPath);
            }
        }


        private String process(Path contextDir, boolean dry) {
            Context context = new Context(contextDir, dry);
            operations.accept(context);
            String result =  String.join("\n", context.steps);
            result = userId != null ? result.replace("${uid}", Integer.toString(userId)) : result;
            Integer gid = getEffectiveGroupId();
            result = gid != null ? result.replace("${gid}", Integer.toString(gid)) : result;
            return result;
        }

        private void copyFileInContext(Path file, String containerPath, Context context, boolean nonRoot, boolean optional) {
            if (!Files.exists(file)) {
                String msg = "File " + file + " not found creating Dockerfile entry " + containerPath + " in "
                        + context.dir;
                if (!optional && !context.dry) {
                    throw new IllegalArgumentException(msg);
                } else {
                    JkLog.verbose(msg);
                }
            }
            String candidateFileName = file.getFileName().toString();
            while (context.importedFileNames.contains(candidateFileName)) {
                candidateFileName = "_" + candidateFileName;
            }
            Path tempPath = context.dir.resolve("imported-files").resolve(candidateFileName);
            context.importedFileNames.add(candidateFileName);
            String ownArg = nonRoot ? "--chown=nonroot:nonrootg " : "";
            context.add("COPY " + ownArg + context.dir.relativize(tempPath).toString().replace('\\', '/')
                    + " " + containerPath);
            if (!context.dry) {
                JkUtilsPath.createDirectories(tempPath.getParent());
                if (Files.isDirectory(file)) {
                    JkPathTree.of(file).copyTo(tempPath, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    JkUtilsPath.copy(file, tempPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }

        }
    }

    public static class Context {

        private final List<String> steps = new LinkedList<>();

        /**
         * The context dir where files and Dockerfile will be generated
         */
        public final Path dir;

        /**
         * If dry = true, this mean we want only rendering rhe Dockerfile as text. Thus
         * some expansive copy operation can be avoided.
         */
        public final boolean dry;

        private final Set<String> importedFileNames = new HashSet<>();

        public Context(Path dir, boolean dry) {
            this.dir = dir;
            this.dry = dry;
        }

        public Context add(String step) {
            steps.add(step);
            return this;
        }

        public Context insertBefore(String startingWith, String ...stepsToInsert) {
            ListIterator<String> listIterator = this.steps.listIterator();
            while (listIterator.hasNext()) {
                String nextStep = listIterator.next();
                if (nextStep.startsWith(startingWith)) {
                    listIterator.previous();
                    for (String stepToInsert : stepsToInsert) {
                        listIterator.add(stepToInsert);
                    }
                    return this;
                }
            }
            return this;
        }
    }

}



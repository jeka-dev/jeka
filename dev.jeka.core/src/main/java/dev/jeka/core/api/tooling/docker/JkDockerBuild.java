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
import dev.jeka.core.api.utils.JkUtilsObject;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Docker Builder assistant for defining and creating Docker images.
 * <p>
 * This class facilitates the programmatic construction of a Docker build context directory
 * and the execution of the <code>docker build</code> command using that context.
 * </p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li>Configuring the base image</li>
 *   <li>Adding a non-root user to the image</li>
 *   <li>Customizing the build process with additional steps in the generated Dockerfile</li>
 * </ul>
 *
 * <h2>Docker Image Build Process</h2>
 * <ol>
 *   <li>
 *     A static template of the Dockerfile is generated. This template may include tokens such as
 *     <i>${baseImage}</i>, allowing flexibility when the base image or user creation steps
 *     need to be determined dynamically.
 *   </li>
 *   <li>
 *     Tokens in the static template are resolved dynamically to produce the final Dockerfile.
 *   </li>
 *   <li>
 *     The build context directory is created. The generated Dockerfile and any specified files
 *     are copied into this directory.
 *   </li>
 *   <li>
 *     The <code>docker build</code> command is executed using the generated build context directory.
 *     This operation requires Docker CLI (e.g., Docker Desktop) installed on the host machine.
 *   </li>
 * </ol>
 *
 * <h2>Default Dockerfile Template</h2>
 * <p>The default Dockerfile template is as follows:</p>
 * <pre><code>
 * FROM ${baseImage}
 * ${addNonRootUser}
 * </code></pre>
 *
 * <p>
 * Users can customize this template by adding steps to the <code>dockerFileTemplate</code> field.
 * Custom tokens (e.g., <i>${myToken}</i>) can also be defined, and their resolution logic specified
 * using the <code>#addTokenResolver(String, Supplier&lt;String&gt;)</code> method.
 * </p>
 *
 * <h2>Importing Files</h2>
 * <p>
 * Files can be imported into the build context using:
 * </p>
 * <ul>
 *   <li><code>#addFsOperation()</code></li>
 *   <li><code>dockerFileTemplate#addCopy(fileSystemPath, containerPath)</code></li>
 * </ul>
 *
 * <h2>Building and Pushing Images</h2>
 * <p>
 * Once the Docker build is configured, the image can be generated using the
 * <code>buildImage(String imageName)</code> method. The resulting image will be automatically
 * pushed to the Docker registry.
 * </p>
 *
 * <h2>Intermediate Steps</h2>
 * <p>
 * Intermediate steps such as <code>dockerFileTemplate.render()</code>,
 * <code>renderDockerFile()</code>, or <code>generateContextDir()</code> can be used
 * to aid in the configuration of the Docker build process.
 * </p>
 *
 * <h2>Extensibility</h2>
 * <p>
 * This class can be extended to provide a specific default template and additional
 * convenience methods.
 * </p>
 */
public class JkDockerBuild {

    private static final String CREATE_NON_ROOT_TOKEN = "createNonRootUser";

    private static final String SWITCH_NON_ROOT_TOKEN = "switchNonRootUser";

    private static final String CHOWN_DIR_TOKEN = "chownDir";

    private static final String GROUP_ID_TOKEN = "GID";

    private static final String USER_ID_TOKEN = "UID";

    private static final String BASE_IMAGE_TOKEN = "baseImage";

    public final DockerfileTemplate dockerfileTemplate = new DockerfileTemplate();

    private String baseImage = "ubuntu";

    private NonRootUserCreationMode nonRootUserCreationMode = NonRootUserCreationMode.AUTO;

    private int userId = 1001;

    private int groupId = 1002;

    private String addUserStatement;

    private final List<Integer> exposedPorts = new LinkedList<>();

    // Contains the copy or similar operation to be done, next the build context dir is created.
    // The "path" to consume stands for the build context dir.
    private JkConsumers<Path> fsOperations = JkConsumers.of();

    // keeps the imported files into a set avoid duplicate names
    private final Set<String> importedFiles = new HashSet<>();

    // The supplied object will be turned in string via #toString()
    private final Map<String, Supplier<Object>> tokenResolvers = new HashMap<>();

    /**
     * Defines the modes for creating a nonroot user in a Docker image.<br/>
     */
    public enum NonRootUserCreationMode {

        /** Always create a nonroot user */
        ALWAYS,

        /** Never create a nonroot user */
        NEVER,

        /**
         * Creates a nonroot user if it cannot be determined whether the base image already includes one.
         * The determination is based on the image name.
         */
        AUTO
    }

    public enum AddUserStatement {

        TEMURIN("RUN addgroup --gid ${GID} nonrootgroup && \\\n" +
                "    adduser --uid ${UID} --gid ${GID} --disabled-password --gecos \"\" nonroot"),

        UBUNTU("RUN groupadd --gid ${GID} nonrootgroup && \\\n" +
                "    useradd --uid ${UID} --gid ${GID} --create-home nonroot && passwd -d nonroot"),

        ALPINE("RUN addgroup --gid ${GID} nonrootgroup && \\\n" +
                "    adduser --uid ${UID} -g ${GID} --disabled-password nonroot");

        public final String statement;

        AddUserStatement(String statement) {
            this.statement = statement;
        }

    }

    /**
     * Creates a {@link JkDockerBuild instannce.}
     */
    public static JkDockerBuild of() {
        return new JkDockerBuild();
    }

    protected JkDockerBuild() {
        dockerfileTemplate
                .add("FROM " + decorateToken(BASE_IMAGE_TOKEN))
                .add(decorateToken(CREATE_NON_ROOT_TOKEN))
                .add(decorateToken(SWITCH_NON_ROOT_TOKEN));

        tokenResolvers.put(BASE_IMAGE_TOKEN, this::getBaseImage);
        tokenResolvers.put(USER_ID_TOKEN, () -> this.userId);
        tokenResolvers.put(GROUP_ID_TOKEN, () -> this.groupId);
        tokenResolvers.put(CREATE_NON_ROOT_TOKEN, this::userCreationStatement);
        tokenResolvers.put(SWITCH_NON_ROOT_TOKEN, () -> hasNonRootUserCreation() ? "USER nonroot" : "");
        tokenResolvers.put(CHOWN_DIR_TOKEN, this::resolveChownDir);
    }

    // ---------------- Getters / setters -----------------------------

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

    public final JkDockerBuild setNonRootUserCreationMode(NonRootUserCreationMode nonRootUserCreationMode) {
        this.nonRootUserCreationMode = nonRootUserCreationMode;
        return this;
    }

    /**
     * Checks whether the image includes a step to create a non-root user.
     *
     * @return true if a non-root user creation step is present, false otherwise.
     */
    public final boolean hasNonRootUserCreation() {
        if (nonRootUserCreationMode == NonRootUserCreationMode.NEVER) {
            return false;
        } else if (nonRootUserCreationMode == NonRootUserCreationMode.ALWAYS) {
            return true;
        }
        return !this.baseImage.contains("nonroot");
    }

    /**
     * Sets the Docker statement for adding a 'nonroot' user.<br/>
     * The placeholders "${UID}" and "${GID}" will be replaced with the user ID and group ID
     * configured for this instance, respectively.
     */
    public final JkDockerBuild setAddUserStatement(String addUserTemplate) {
        this.addUserStatement = addUserTemplate;
        return this;
    }

    /**
     * @see #setAddUserStatement(String)
     */
    public final JkDockerBuild setAddUserStatement(AddUserStatement addUserTemplate) {
        return setAddUserStatement(addUserTemplate.statement);
    }

    /**
     * Sets the user ID for the 'nonroot' user.<br/>
     *
     * @param userId the user ID to assign.
     */
    public final JkDockerBuild setUserId(int userId) {
        this.userId = userId;
        return this;
    }

    /**
     * Sets the group ID for the 'nonroot' user.<br/>
     *
     * @param groupId the group ID to assign.
     */
    public final JkDockerBuild setGroupId(int groupId) {
        this.groupId = groupId;
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
     * Returns a string representation of the command line arguments for port mapping in the Docker image.
     */
    protected final String getPortMappingArgs() {
        return cmdLinePortMapping(this.exposedPorts);
    }

    // ------------------- Customize Dockerfile ------------------------------------



    /**
     * Specifies how to resolve the given token. The value supplier can return any type of object,
     * which will be converted to a String using the {@link #toString()} method.
     */
    public JkDockerBuild addTokenResolver(String token, Supplier<Object> valueSupplier) {
        this.tokenResolvers.put(token, valueSupplier);
        return this;
    }

    // ------------------------ customize build context dir --------------------

    /**
     * Adds an operation to be executed immediately after the build context directory is created.<p>
     * This is typically used to import files or directories into the context directory.
     *
     * @param fsOperation a {@link Consumer<Path>} that accepts the path to the build context directory.
     */
    public JkDockerBuild addFsOperation(Consumer<Path> fsOperation) {
        fsOperations.add(fsOperation);
        return this;
    }

    // ------------------------------ Build methods  --------------------------------------------

    /**
     * Render the final Dockerfile.
     */
    public String renderDockerfile() {
        String dockerfileContent = dockerfileTemplate.render(this);
        dockerfileContent = interpolate(dockerfileContent, true);

        // Adds exposed ports to the end of the Dockerfile.
        String exposedPortStatements = dockecBuildExposedPorts(this.exposedPorts);
        if (!exposedPortStatements.isEmpty()) {
            dockerfileContent =  dockerfileContent + "\n" + exposedPortStatements;
        }
        return dockerfileContent;
    }

    /**
     * Builds the Docker image with the specified image name.<br/>
     *
     * @param buildContextDir the directory where the Dockerfile will be generated and from which COPY paths will be resolved.
     * @param imageName the name of the image to build. It may include a tag name.
     */
    public void buildImage(Path buildContextDir, String imageName) {
        this.generateContextDir(buildContextDir);
        JkDocker.of()
                .assertPresent()
                .setInheritIO(true)
                .setLogCommand(true)
                .setLogWithJekaDecorator(true)
                .setInheritIO(false)
                .addParams("build", "-t", imageName, buildContextDir.toString())
                .exec();

        String portMapping = getPortMappingArgs();
        JkLog.info("Run docker image: docker run --rm %s%s", portMapping, imageName);
    }

    /**
     * Similar to {@link #buildImage(Path, String)}, but without requiring a context directory to be specified.<p>
     * A temporary context directory will be created in a random location.
     *
     * @return the path of the created context directory.
     */

    public Path buildImageInTemp(String imageName) {
        Path contextDir = JkUtilsPath.createTempDirectory("jeka-docker-build-context");
        JkLog.verbose("Using context dir %s for building Docker image %s" , contextDir, imageName);
        buildImage(contextDir, imageName);
        return contextDir;
    }

    /**
     * Generates the build context directory without building the Docker image. This can be useful for debugging purposes.<p>
     * This method is automatically called when {@link #buildImage(Path, String)} is invoked.
     */
    public void generateContextDir(Path buildContextDir) {
        String dockerfileContent = renderDockerfile();
        JkPathFile.of(buildContextDir.resolve("Dockerfile")).write(dockerfileContent);
        this.fsOperations.accept(buildContextDir);
    }

    // ------------ utility method -------------

    /**
     * Renders a human-readable string representation of the information used
     * to build the image.
     */
    public String renderInfo() {
        String rule = "-------------------------------------------------";
        StringBuilder sb = new StringBuilder();

        sb.append(rule).append("\n");
        sb.append("Dockerfile Template:\n");
        sb.append(rule).append("\n");
        sb.append(dockerfileTemplate.render(this)).append("\n");

        sb.append(rule).append("\n");
        sb.append("Dockerfile :\n");
        sb.append(rule).append("\n");
        sb.append(renderDockerfile()).append("\n");
        sb.append(rule).append("\n");
        return sb.toString();
    }

    static String decorateToken(String token) {
        return "${" + token + "}";
    }

    protected String computeImportedFileRelativePath(Path fileToCopy, String targetDirRelPath) {
        String candidateFileName = targetDirRelPath + "/" + fileToCopy.getFileName().toString();
        while (importedFiles.contains(candidateFileName)) {
            candidateFileName = "_" + candidateFileName;
        }
        return candidateFileName;
    }

    /**
     * Transform a list as [a,b,c] in a string as <i>"a", "b", "c"</i>
     */
    protected static String toDoubleQuotedArgs(String ...args) {
        StringBuilder sb = new StringBuilder();
        Arrays.stream(args).forEach(arg -> sb.append("\"").append(arg).append("\", "));
        if (args.length > 0) {
            sb.setLength(sb.length() - 2);
        }
        return sb.toString();
    }

    /**
     * @see #toDoubleQuotedArgs(String...)
     */
    protected static String toDoubleQuotedArgs(List<String> args) {
        return toDoubleQuotedArgs(args.toArray(new String[0]));
    }

    //----------------------  private methods --------------------------------------------------

    private String interpolate(String dockerFileCandidate, boolean goRecursive) {
        String result = dockerFileCandidate;
        for (Map.Entry<String, Supplier<Object>> entry : tokenResolvers.entrySet()) {
            String token = decorateToken(entry.getKey());
            String value = JkUtilsObject.toString(entry.getValue().get());
            result = result.replace(token, value);
        }
        int tokenCount = JkUtilsString.countOccurrence(result, '{');
        if (tokenCount == 0 || !goRecursive) {
            return result;
        }
        String redoResult = interpolate(result, false);
        if (redoResult.equals(redoResult)) {
            return result;
        }
        return interpolate(result, true);
    }

    private String userCreationStatement() {
        if (!this.hasNonRootUserCreation()) {
            return "";
        }
        String template = inferAddUserTemplate();
        template = template
                .replace(decorateToken(USER_ID_TOKEN), Integer.toString(userId))
                .replace(decorateToken(GROUP_ID_TOKEN), Integer.toString(groupId));
        return template;
    }

    private static String toString(List<String> statements) {
        return statements.stream().reduce("", (init, step) -> init + step + "\n" );
    }

    private String inferAddUserTemplate() {
        if (!JkUtilsString.isBlank(addUserStatement)) {
            return addUserStatement;
        }
        if (baseImage.contains("alpine")) {    // should be tested first as 'alpine' may also appear in temurin based image.
            return AddUserStatement.ALPINE.statement;
        } else if (baseImage.contains("ubuntu")) {
            return AddUserStatement.UBUNTU.statement;
        } else if (baseImage.contains("temurin")) {
            return AddUserStatement.TEMURIN.statement;
        }
        return AddUserStatement.UBUNTU.statement;
    }

    private String mkdirStatement(String dirPath) {
        return String.format("mkdir -p %s ${%s}%s", dirPath, CHOWN_DIR_TOKEN, dirPath);
    }

    private String resolveChownDir() {
        return hasNonRootUserCreation() ? "&& chown -R nonroot:nonrootgroup " : "";
    }



    private void importFile(Path buildContextDir, String relativeInsidePath, Path ousideContextPath, boolean optional) {
        if (!optional && !Files.exists(ousideContextPath)) {
            String msg = "File " + ousideContextPath + " not found while creating a Dockerfile entry in "
                    + buildContextDir +  ".";
            throw new IllegalArgumentException(msg);
        }
        Path insideContextPath = buildContextDir.resolve(relativeInsidePath);
        JkUtilsPath.createDirectories(insideContextPath.getParent());
        if (Files.isDirectory(ousideContextPath)) {
            JkPathTree.of(ousideContextPath).copyTo(insideContextPath, StandardCopyOption.REPLACE_EXISTING);
        } else {
            JkUtilsPath.copy(ousideContextPath, insideContextPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }


    static String dockecBuildExposedPorts(List<Integer> exposedPorts) {
        return exposedPorts.isEmpty() ? "" :
                "EXPOSE " + exposedPorts.stream().map(i -> Integer.toString(i))
                        .collect(Collectors.joining(" "));
    }

    static String cmdLinePortMapping(List<Integer> exposedPorts) {
        String portMapping = "";
        if (!exposedPorts.isEmpty()) {
            portMapping = exposedPorts.stream()
                    .map(Object::toString)
                    .reduce("-p ", (init, port) -> init  + port + ":" + port + " ");
        }
        return portMapping;
    }

    public class DockerfileTemplate {

        private int index = 0;

        private final List<String> buildSteps = new LinkedList<>();

        private DockerfileTemplate() {
        }

        /**
         * Appends the specified step at the end of step container.
         */
        public final DockerfileTemplate add(String step) {
            buildSteps.add(index, step);
            index++;
            return this;
        }

        /**
         * Removes the build step currently at cursor position.
         */
        public final DockerfileTemplate remove() {
            buildSteps.remove(index);
            return this;
        }

        /**
         * A convenient method to add a COPY step for a file or directory located outside the Docker build context.<p>
         * The specified file or directory will be copied into the Docker build context, and a corresponding COPY step
         * will be added to the Dockerfile.
         *
         * @param chownNonroot if true, includes the '--chown=nonroot:nonrootg' option in the COPY statement.
         * @param optional if false, an error is thrown if the specified file or directory does not exist.
         */
        public final DockerfileTemplate addCopy(Path fileOrDirToCopy, String containerPath, boolean chownNonroot, boolean optional) {
            String insidePath = computeImportedFileRelativePath(fileOrDirToCopy, "imported-files");
            fsOperations.add(buildContextDir -> {
                importFile(buildContextDir, insidePath, fileOrDirToCopy, false);
            });
            String ownArg = chownNonroot ? "--chown=nonroot:nonrootg " : "";
            return add("COPY " + ownArg + insidePath + " " + containerPath);
        }

        /**
         * @see #addCopy(Path, String, boolean, boolean)
         */
        public DockerfileTemplate addCopy(Path fileOrDirToCopy, String containerPath, boolean chownNonroot) {
            return addCopy(fileOrDirToCopy, containerPath, chownNonroot, false);
        }

        /**
         * @see #addCopy(Path, String, boolean, boolean)
         */
        public DockerfileTemplate addCopy(Path fileOrDirToCopy, String containerPath) {
            return addCopy(fileOrDirToCopy, containerPath, false, false);
        }

        /**
         * Adds a Dockerfile step that creates a directory, belonging to the 'nonroot' user if such a
         * user exists.
         */
        public DockerfileTemplate addNonRootMkDirs(String dirPath, String ...extraDirs) {
            StringBuilder sb = new StringBuilder("RUN ");
            sb.append(mkdirStatement(dirPath));
            for (String extraDir : extraDirs) {
                sb.append(" \\\n    && ");
                sb.append(mkdirStatement(extraDir));
            }
            return add(sb.toString());
        }

        /**
         * Adds an entrypoint build step
         */
        public DockerfileTemplate addEntrypoint(String ...args) {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            sb.append(toDoubleQuotedArgs(args));
            sb.append("]");
            return add("ENTRYPOINT " + sb);
        }

        // ------------------------------ Move Cursor ---------------------------------------------

        /**
         * Positions the cursor just before the first build step that begins with the specified prefix.<p>
         * Any subsequent build steps added will be appended starting from this position.
         *
         * @param prefix the prefix of the build step to locate
         * @return the updated builder or context for chaining
         */
        public DockerfileTemplate moveCursorBefore(String prefix) {
            int i = 0;
            for (String step : buildSteps) {
                if (step.startsWith(prefix)) {
                    this.index = i;
                    return this;
                }
                i++;
            }
            throw noSuchStepFoundException(prefix);
        }


        public DockerfileTemplate moveCursorNext() {
            this.index++;
            return this;
        }

        /**
         * Moves the cursor just before ${switchNonRootUser} token.
         *
         * @see #moveCursorBefore(String)
         */
        public DockerfileTemplate moveCursorBeforeUserNonRoot() {
            return moveCursorBefore(decorateToken(SWITCH_NON_ROOT_TOKEN));
        }

        /**
         * Renders the Dockerfile template, with tokens still present.
         *
         * @param jkDockerBuild
         */
        public String render(JkDockerBuild jkDockerBuild) {
            return String.join("\n", buildSteps);
        }

        private IllegalArgumentException noSuchStepFoundException(String step) {
            return new IllegalArgumentException("No step starting with " + step + " found. " +
                    "Existing steps are:\n" + String.join("\n", buildSteps));
        }
    }

}



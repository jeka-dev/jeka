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

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkConstants;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Docker Builder assistant for creating Docker images with nonroot user.
 */
public class JkDockerBuild {

    public static final String TEMURIN_ADD_USER_TEMPLATE = "RUN addgroup --gid ${GID} nonrootgroup && \\\n" +
            "    adduser --uid ${UID} --gid ${GID} --disabled-password --gecos \"\" nonroot";

    public static final String UBUNTU_ADD_USER_TEMPLATE = "RUN addgroup --gid ${GID} nonrootgroup && \\\n" +
            "    adduser --uid ${UID} --gid ${GID} --disabled-password --gecos \"\" nonroot";

    public static final String ALPINE_ADD_USER_TEMPLATE = "RUN addgroup --gid ${GID} nonrootgroup && \\\n" +
            "    adduser --uid ${UID} -g ${GID} --disabled-password --gecos \"\" nonroot\n";


    private static final String EXTRA_FILE_DIR = "extra-files";

    protected JkRepoSet repos = JkRepo.ofMavenCentral().toSet();

    private String baseImage = "ubuntu";

    private Integer userId = 1001;

    private Integer groupId;

    private String addUserTemplate;

    private Path baseBuildDir;

    // key is destination within image filesystem, value is path in host filesystem
    protected final Map<String, Path> nonRootExtraFiles = new HashMap<>();

    private final List<String> preUserCreationStatements = new LinkedList<>();

    private final List<String> nonRootStatements = new LinkedList<>();

    private final List<String> rootStatements = new LinkedList<>();

    private final List<Integer> exposedPorts = new LinkedList<>();

    private final List<String> footerStatements = new LinkedList<>();


    protected JkDockerBuild() {
    }

    /**
     * Creates a {@link JkDockerBuild instannce.}
     */
    public static JkDockerBuild of() {
        return new JkDockerBuild();
    }

    /**
     * Sets the base image name to use a s the base image in the docker file.
     */
    public final JkDockerBuild setBaseImage(String baseImage) {
        this.baseImage = baseImage;
        return this;
    }

    /**
     * Sets the base build directory for the Docker build process.
     * This directory will be used as the context directory for the Dockerfile.
     */
    public final JkDockerBuild setBaseBuildDir(Path baseBuildDir) {
        this.baseBuildDir = baseBuildDir;
        return this;
    }

    /**
     * Adds a the specified file at the specified location in the image to construct.
     * @param file The path of the file in the host file system
     * @param destination The path of the file in the image.
     */
    public final JkDockerBuild addExtraFile(Path file, String destination) {
        this.nonRootExtraFiles.put(destination, file);
        return this;
    }

    /**
     * Sets the Maven repository used for downloading agent jars. Initial value is
     * Maven central.
     */
    public final JkDockerBuild setDownloadMavenRepos(JkRepoSet repos) {
        this.repos = repos;
        return this;
    }

    /**
     * Adds a docker build statement to be executed at the start of Docker build.
     */
    public final JkDockerBuild addNonRootStatement(String statement) {
        this.nonRootStatements.add(statement);
        return this;
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
     * Adds a docker build statement at the end of the DockerBuild file
     */
    public final JkDockerBuild addFooterStatement(String statement) {
        this.footerStatements.add(statement);
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
     * @param imageName The name of the image to be build. It may contains or not a tag name.
     */
    public final void buildImage(String imageName) {
        JkDocker.assertPresent();
        Path tempBuildDir = getTempBuildDir(imageName);
        JkPathTree.of(tempBuildDir).deleteContent();

        String dockerBuild = dockerBuildContent(imageName);

        JkPathFile.of(tempBuildDir.resolve("Dockerfile")).write(dockerBuild);


        // Build image using Docker daemon
        JkProcess.of("docker", "build", "-t", imageName, "./" + tempBuildDir).setInheritIO(true)
                .setLogCommand(true)
                .setLogWithJekaDecorator(true)
                .setInheritIO(false)
                .exec();

        String portMapping = portMappingArgs();
        JkLog.info("Run docker image : docker run -it --rm %s%s", portMapping, imageName);
        JkLog.info("Use '-e JVM_OPTIONS=...' and '-e PROGRAM_ARGS=...' options to pass JVM options or program arguments.");
    }

    /**
     * Computes a list of statements to be added before the footer statements in the Docker build file..
     */
    protected List<String> computePreFooterStatements(String imageName) {
        return Collections.emptyList();
    }

    // non-private for testing purpose
    final String dockerBuildContent(String imageName) {
        // Handle extra files
        Map<Path, Path> extraNonRootFileMap = copyExtraFilesToBuildDir(imageName, nonRootExtraFiles);
        String nonRootExtraFileCopy = createCopyStatements(extraNonRootFileMap);


        // Handle exposed ports
        String exposedPortStatements = exposedPorts.isEmpty() ? "" :
                "EXPOSE " + exposedPorts.stream().map(i -> Integer.toString(i))
                        .collect(Collectors.joining(" ")) + "\n";

        String dockerBuildTemplate =
                "FROM ${baseImage}\n" +
                        exposedPortStatements +
                        toString(preUserCreationStatements) +
                        userCreationStatement() +
                        appDirCreation() +
                        toString(rootStatements) +
                        switchUser() +
                        "WORKDIR /app\n" +
                        nonRootExtraFileCopy + "\n" +
                        toString(computePreFooterStatements(imageName)) +
                        toString(footerStatements);
        return dockerBuildTemplate.replace("${baseImage}", baseImage);
    }

    /**
     * Returns a formatted string that contains information about this Docker build.
     */
    public String info(String imageName) {

        StringBuilder sb = new StringBuilder();
        sb.append("Dockerbuild File  : ").append("\n");
        sb.append("----------------------------------------------").append("\n");
        sb.append(this.dockerBuildContent(imageName)).append("\n");
        sb.append("----------------------------------------------").append("\n");
        sb.append("Base Image Name   : " + this.baseImage).append("\n");

        sb.append("Exposed Ports     : " + this.exposedPorts).append("\n");

        if (this.userId == null) {
            sb.append("Use               : root\n");
        } else {
            sb.append("User              : " + userId).append("\n");
            sb.append("Group             : " + groupId(userId)).append("\n");
        }

        sb.append("Extra Build Steps : ").append("\n");
        this.nonRootStatements.forEach(item -> sb.append("  " + item).append("\n"));

        sb.append("Extra Files       :").append("\n");
        this.nonRootExtraFiles.entrySet().forEach(entry
                -> sb.append("  " + entry.getValue()  + " -> " + entry.getKey()+ "\n"));
        return sb.toString();
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

    protected final Path getTempBuildDir(String imageName) {
        String dirName = "docker-build-" + imageName.replace(':', '#');
        Path parent = baseBuildDir == null ? Paths.get(JkConstants.OUTPUT_PATH)
                : baseBuildDir;
        return parent.resolve(dirName);

    }

    private Map<Path, Path> copyExtraFilesToBuildDir(String imageName, Map<String, Path> extraFiles) {
        Map<Path, Path> result = new HashMap<>();
        Path extraFileDir = getTempBuildDir(imageName).resolve(EXTRA_FILE_DIR);
        if (!extraFiles.isEmpty()) {
            JkUtilsPath.createDirectories(extraFileDir);
        }
        for (Map.Entry<String, Path> entry : extraFiles.entrySet()) {
            Path file = entry.getValue();
            Path dest = extraFileDir.resolve(file.getFileName());
            if (Files.exists(dest)) {
                dest = extraFileDir.resolve(file.getFileName() + "-" + UUID.randomUUID());
            }
            result.put(file, dest);
            if (!Files.exists(file)) {
                throw new IllegalStateException(String.format("Docker import file %s -> %s, does not exist.",
                        file, entry.getKey()));
            }
            JkUtilsPath.copy(file, dest, StandardCopyOption.REPLACE_EXISTING);
        }
        return result;
    }

    private String createCopyStatements(Map<Path, Path> buildDirMap) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Path> entry : nonRootExtraFiles.entrySet()) {
            Path buildDirFile = buildDirMap.get(entry.getValue());
            String from =  EXTRA_FILE_DIR + "/" + buildDirFile.getFileName();
            String to = entry.getKey();
            sb.append("COPY " + from + " " + to + "\n");
        }
        return sb.toString();
    }

    private String userCreationStatement() {
        if (userId == null) {
            return "";
        }
        int gid = groupId(userId);
        String template = getAddUserTemplate() + "\n";
        return template.replace("${UID}", userId.toString()).replace("${GID}", Integer.toString(gid));
    }

    private String appDirCreation() {
        if (userId == null) {
            return "RUN mkdir -p /app\n";
        }
        int gid = groupId(userId);
        String template =  "RUN mkdir -p /app && \\\n" +
                "    chown -R ${UID}:${GID} /app \n";
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



}



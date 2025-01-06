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
import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.project.JkBuildable;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Docker Builder assistant for creating Docker images that run JVM applications.<br/>
 *
 * Provides functionality tailored to JVM programs, such as setting classes, classpaths,
 * agents, and Maven repositories for fetching tools.
 */
public class JkDockerJvmBuild extends JkDockerBuild {

    public enum PopularBaseImage {

        TEMURIN_JRE_MINIMAL_IMAGE ("eclipse-temurin:23-jre-ubi9-minimal"),

        TEMURIN_JRE_ALPINE_IMAGE("eclipse-temurin:23-jre-alpine");

        public final String imageName;

        PopularBaseImage(String imageName) {
            this.imageName = imageName;
        }
    }

    public static final String DEFAULT_BASE_IMAGE = PopularBaseImage.TEMURIN_JRE_ALPINE_IMAGE.imageName;

    private static final Predicate<Path> CHANGING_LIB = path -> path.toString().endsWith("-SNAPSHOT.jar");

    private static final JkPathMatcher CLASS_MATCHER = JkPathMatcher.of("**/*.class", "*.class");

    private JkPathTree classes;

    private List<Path> classpath = Collections.emptyList();

    private String mainClass;

    protected JkRepoSet repos = JkRepo.ofMavenCentral().toSet();

    //. agent jar or coordinate -> agent option string
    private final Map<PathOrCoordinate, String> agents = new HashMap<>();

    private JkDockerJvmBuild() {

        // Create template
        this.setBaseImage(DEFAULT_BASE_IMAGE);
        dockerfileTemplate
                .moveCursorBeforeUserNonRoot()
                .addNonRootMkDirs("/app", "/workdir")
                .moveCursorNext()  // move after user switch
                .add("COPY agents /app/agents")
                .add("COPY libs /app/libs")
                .add("COPY snapshot-libs /app/snapshot-libs")
                .add("COPY classpath.txt /app/classpath.txt")
                .add("COPY resources /app/classes")
                .add("COPY classes /app/classes")

                .add("WORKDIR /workdir")

                 // Create command line
                .add("ENTRYPOINT [ ${javaCmdLine} ]")
                .add("CMD []");

        // Resolve template tokens
        addTokenResolver("javaCmdLine", this::javaCommandLine);

        // Create needed files in build context dir
        addFsOperation(this::importFiles);
    }

    /**
     * Creates a {@link JkDockerJvmBuild instannce.}
     */
    public static JkDockerJvmBuild of() {
        return new JkDockerJvmBuild();
    }

    /**
     * Creates a JkDockerBuild instance for the specified JkProject.
     */
    public static JkDockerJvmBuild of(JkBuildable buildable) {
        return of().adaptTo(buildable);
    }

    @Override
    public void buildImage(Path contextDir, String imageName) {
        super.buildImage(contextDir, imageName);
        JkLog.info("- Pass additional JVM options using the '-e JAVA_TOOL_OPTIONS=...' option.");
        JkLog.info("- Pass program arguments by appending them to the end of the command line.");
        JkLog.info("- Map the host's current directory to the container's working directory using the '-v $PWD:/workdir' option.");

    }

    /**
     * Adapts this JkDockerBuild instance to build the specified JkProject.
     */
    public JkDockerJvmBuild adaptTo(JkBuildable buildable) {
        JkPathTree classTree = JkPathTree.of(buildable.getClassDir());
        if (!classTree.withMatcher(JkPathMatcher.of("**/*class")).containFiles()) {
            JkLog.verbose("No compiled classes found. Force compile.");
            buildable.compileIfNeeded();
        }
        String mainClass = buildable.getMainClass();
        JkUtilsAssert.state(mainClass != null, "No main class has been defined or found on this project. Please, set the @project.pack.mainClass property.");
        return this
                .setClasses(classTree)
                .setClasspath(buildable.getRuntimeDependenciesAsFiles())
                .setMainClass(mainClass);
    }

    /**
     * Sets the compiled Java classes that constitute the Java program to be executed.
     */
    public JkDockerJvmBuild setClasses(JkPathTree classes) {
        this.classes = classes;
        return this;
    }

    /**
     * Sets the jar files that constitute the classpath of the program to be executed.
     */
    public JkDockerJvmBuild setClasspath(List<Path> classpath) {
        this.classpath = classpath;
        return this;
    }

    /**
     * Specifies the main class to run to execute the Program.
     */
    public JkDockerJvmBuild setMainClass(String mainClass) {
        this.mainClass = mainClass;
        return this;
    }

    /**
     * Adds the specified agent to the running JVM
     * @param file The agent jar file in the host system
     * @param agentOptions The agent options that will be prepended to '-javaagent:' option
     */
    public JkDockerJvmBuild addAgent(Path file, String agentOptions) {
        this.agents.put(new PathOrCoordinate(file), agentOptions);
        return this;
    }



    /**
     * Adds the specified agent to the running JVM
     * @param agentCoordinate The agent maven coordinate to download agent.
     * @param agentOptions The agent options that will be prepended to '-javaagent:' option
     */
    public JkDockerJvmBuild addAgent(@JkDepSuggest String agentCoordinate, String agentOptions) {
        this.agents.put(new PathOrCoordinate(agentCoordinate), agentOptions);
        return this;
    }

    /**
     * Adds the specified agent to the running JVM, without passing any option.
     * @param agentCoordinate The agent maven coordinate to download agent.
     */
    public JkDockerJvmBuild addAgent(@JkDepSuggest String agentCoordinate) {
        return addAgent(agentCoordinate, "");
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
     * Returns a formatted string that contains information about this Docker build.
     */
    @Override
    public String renderInfo() {
        StringBuilder sb = new StringBuilder();

        sb.append("Agents            : ").append("\n");
        this.agents.entrySet().forEach(entry
                -> sb.append("  " + entry.getKey()  + "=" + entry.getValue() + "\n"));
        List<Path> libs = sanitizedLibs();
        sb.append("Released libs     :").append("\n");
        libs.stream().filter(CHANGING_LIB.negate()).forEach(item -> sb.append("  " + item + "\n"));
        sb.append("Snapshot libs     :").append("\n");
        libs.stream().filter(CHANGING_LIB).forEach(item -> sb.append("  " + item + "\n"));

        sb.append("Resources Files   : " ).append("\n");
        resourceFiles().getRelativeFiles().forEach(path -> sb.append("  " + path + "\n"));

        sb.append("Class Files       : " ).append("\n");
        classFiles().getRelativeFiles().forEach(path -> sb.append("  " + path + "\n"));

        sb.append("Main class        : " + this.mainClass).append("\n");
        sb.append("Classpath         : " ).append("\n");
        this.classpath.forEach(item -> sb.append("  " + item + "\n"));
        sb.append(super.renderInfo());
        return sb.toString();
    }

    private String javaCommandLine() {
        List<String> cmdLine = new ArrayList<>();
        cmdLine.add("java");
        cmdLine.addAll(computeAgentArgumentsAsList());
        cmdLine.add("-cp");
        cmdLine.add("@/app/classpath.txt");
        cmdLine.add(mainClass);
        return toDoubleQuotedArgs(cmdLine);
    }

    private void importFiles(Path buildContextDir) {
        Map<PathOrCoordinate, String> agentFileContainerPaths = computeAgentFileContainerPaths();

        // Handle Java layers
        List<Path> sanitizedLibs = sanitizedLibs();

        // -- Create non-snapshot layer
        Path libs = buildContextDir.resolve("libs");
        JkUtilsPath.createDirectories(libs);
        sanitizedLibs.stream()
                .filter(path -> !CHANGING_LIB.test(path))
                .map(JkPathFile::of)
                .forEach(file -> file.copyToDir(libs, StandardCopyOption.REPLACE_EXISTING));


        // -- Create snapshot layer
        Path snapshotLibs = buildContextDir.resolve("snapshot-libs");
        JkUtilsPath.createDirectories(snapshotLibs);
        sanitizedLibs.stream()
                .filter(CHANGING_LIB)
                .map(JkPathFile::of)
                .forEach(file -> file.copyToDir(snapshotLibs, StandardCopyOption.REPLACE_EXISTING));


        // -- Add classpath.txt layer
        Path classpathTxt =buildContextDir.resolve("classpath.txt");
        String classpathTxtContent = createClasspathArgs(sanitizedLibs);
        JkPathFile.of(classpathTxt).deleteIfExist().write(classpathTxtContent);


        // -- add resource layer
        Path resources = buildContextDir.resolve("resources");
        JkUtilsPath.createDirectories(resources);
        resourceFiles().copyTo(resources, StandardCopyOption.REPLACE_EXISTING);


        // -- add class layer
        Path classes = buildContextDir.resolve("classes");
        JkUtilsPath.createDirectories(classes);
        classFiles().copyTo(classes, StandardCopyOption.REPLACE_EXISTING);


        // Handle agents
        Path agentDir = buildContextDir.resolve("agents");
        JkUtilsPath.createDirectories(agentDir);
        agentFileContainerPaths.forEach( (fileOrCoordinate, containerPath) -> {
            JkPathFile.of(fileOrCoordinate.getOrFetch()).copyToDir(agentDir, StandardCopyOption.REPLACE_EXISTING);
        });
    }

    private List<String> computeAgentArgumentsAsList() {
        Map<PathOrCoordinate, String> agentFileContainerPaths = computeAgentFileContainerPaths();
        List<String> result = new ArrayList<>();
        for (Map.Entry<PathOrCoordinate, String> entry : agents.entrySet()) {
            String agentPath =  agentFileContainerPaths.get(entry.getKey());
            String arg = "-javaagent:" + agentPath;
            if (!JkUtilsString.isBlank(entry.getValue())) {
                arg = arg + "=" + entry.getValue();
            }
            result.add(arg);
        }
        return result;
    }

    private Map<PathOrCoordinate, String> computeAgentFileContainerPaths() {
        Map<PathOrCoordinate, String> result = new HashMap<>();
        for (Map.Entry<PathOrCoordinate, String> entry : this.agents.entrySet()) {
            String fileName = entry.getKey().toPathLocation().getFileName().toString();
            result.put(entry.getKey(), "/app/agents/" + fileName);
        }
        return result;
    }


    private JkPathTree classFiles() {
        return classes.andMatcher(CLASS_MATCHER);
    }

    private JkPathTree resourceFiles() {
        return classes.andMatcher(CLASS_MATCHER.negate());
    }

    private List<Path> sanitizedLibs() {
        return classpath.stream()
                .filter(path -> !Files.isDirectory(path))
                .collect(Collectors.toList());
    }

    private String createClasspathArgs(List<Path> sanitizedLibs) {
        StringBuilder classpathValue = new StringBuilder();
        if (classes != null && classes.containFiles()) {
            classpathValue.append("/app/classes:/app/resources");
        }
        sanitizedLibs.forEach(path -> classpathValue.append(":/app/libs/" + path.getFileName()));
        return classpathValue.toString();
    }

    private class PathOrCoordinate {

        private final Path path;

        private final JkCoordinate coordinate;

        private PathOrCoordinate(Path path, JkCoordinate coordinate) {
            this.path = path;
            this.coordinate = coordinate;
        }

        PathOrCoordinate(Path path) {
            this(path, null);
        }

        PathOrCoordinate(String coordinate) {
            this(null, JkCoordinate.of(coordinate));
        }

        // Get path without downloading
        Path toPathLocation() {
            if (path != null) {
                return path;
            }
            return coordinate.cachePath();
        }

        Path getOrFetch() {
            if (path != null) {
                return path;
            }
            return JkCoordinateFileProxy.of(repos, coordinate).get();
        }

        @Override
        public final boolean equals(Object o) {
            if (!(o instanceof PathOrCoordinate)) return false;

            PathOrCoordinate that = (PathOrCoordinate) o;
            return Objects.equals(path, that.path) && Objects.equals(coordinate, that.coordinate);
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(path);
            result = 31 * result + Objects.hashCode(coordinate);
            return result;
        }

        @Override
        public String toString() {
            return path != null ? path.toString() : coordinate.toString();
        }
    }
}



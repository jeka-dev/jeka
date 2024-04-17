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
import dev.jeka.core.api.project.JkProject;
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
 * Docker Builder assistant for creating Docker images running Java programs.
 */
public class JkDockerJvmBuild extends JkDockerBuild {

    public static final String BASE_IMAGE = "eclipse-temurin:21.0.2_13-jdk-jammy";

    private static final Predicate<Path> CHANGING_LIB = path -> path.toString().endsWith("-SNAPSHOT.jar");

    private static final JkPathMatcher CLASS_MATCHER = JkPathMatcher.of("**/*.class", "*.class");

    private String baseImage = BASE_IMAGE;

    private JkPathTree classes;

    private List<Path> classpath = Collections.emptyList();

    private String mainClass;

    private final Map<Object, String> agents = new HashMap<>();

    private final List<String> extraBuildSteps = new LinkedList<>();

    private final List<Integer> exposedPorts = new LinkedList<>();

    private JkDockerJvmBuild() {
        this.setBaseImage(BASE_IMAGE);

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
    public static JkDockerJvmBuild of(JkProject project) {
        return of().adaptTo(project);
    }

    /**
     * Adapts this JkDockerBuild instance to build the specified JkProject.
     */
    public JkDockerJvmBuild adaptTo(JkProject project) {
        JkPathTree classTree = JkPathTree.of(project.compilation.layout.resolveClassDir());
        if (!classTree.withMatcher(JkPathMatcher.of("**/*class")).containFiles()) {
            JkLog.verbose("No compiled classes found. Force compile.");
            project.compilation.runIfNeeded();
        }
        String mainClass = project.packaging.getMainClass();
        this.setBaseBuildDir(project.compilation.layout.getOutputDir());
        JkUtilsAssert.state(mainClass != null, "No main class has been defined or found on this project. Set the @project.pack.mainClass property");
        return this
                .setClasses(classTree)
                .setClasspath(project.packaging.resolveRuntimeDependenciesAsFiles())
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
        this.agents.put(file, agentOptions);
        return this;
    }

    /**
     * Adds the specified agent to the running JVM
     * @param agentCoordinate The agent maven coordinate to download agent.
     * @param agentOptions The agent options that will be prepended to '-javaagent:' option
     */
    public JkDockerJvmBuild addAgent(@JkDepSuggest String agentCoordinate, String agentOptions) {
        this.agents.put(agentCoordinate, agentOptions);
        return this;
    }

    /**
     * Adds a docker build statement to be executed at the start of Docker build.
     */
    public JkDockerJvmBuild addExtraBuildStep(String statement) {
        this.extraBuildSteps.add(statement);
        return this;
    }

    @Override
    protected List<String> computePreFooterStatements(String imageName) {
        List<String> result = new LinkedList<>();

        // Handle agents
        Map<Object, Path> agentBuildDirMap = copyAgents(imageName);
        String agentCopy = createAgentCopyInstruction(agentBuildDirMap);
        String agentInstructions = createAgentOptions(agentBuildDirMap);
        result.add(agentCopy);

        // Handle Java layers
        String copyJavaFiles =
            "COPY libs /app/libs\n" +
            "COPY snapshot-libs /app/libs\n" +
            "COPY classpath.txt /app/classpath.txt\n" +
            "COPY resources /app/classes\n" +
            "COPY classes /app/classes\n";
        result.add(copyJavaFiles);

        Path tempBuildDir = getTempBuildDir(imageName);
        List<Path> sanitizedLibs = sanitizedLibs();
        JkUtilsPath.createDirectories(tempBuildDir.resolve("libs"));
        JkUtilsPath.createDirectories(tempBuildDir.resolve("snapshot-libs"));
        JkUtilsPath.createDirectories(tempBuildDir.resolve("resources"));
        JkUtilsPath.createDirectories(tempBuildDir.resolve("classes"));

        // -- Copy classes to docker-build/classes
        classFiles().copyTo(tempBuildDir.resolve("classes"), StandardCopyOption.REPLACE_EXISTING);

        //  -- Copy resources to docker-build/resources
        resourceFiles().copyTo(tempBuildDir.resolve("resources"), StandardCopyOption.REPLACE_EXISTING);

        // -- Copy classpath.txt
        String classpathTxt = createClasspathArs(sanitizedLibs);
        JkPathFile.of(tempBuildDir.resolve("classpath.txt")).deleteIfExist().write(classpathTxt);

        // -- Copy Snapshot libs
        sanitizedLibs.stream()
                .filter(CHANGING_LIB)
                .map(JkPathFile::of)
                .forEach(file -> file.copyToDir(tempBuildDir.resolve("libs")));

        // -- Copy libs
        sanitizedLibs.stream()
                .filter(path -> !CHANGING_LIB.test(path))
                .map(JkPathFile::of)
                .forEach(file -> file.copyToDir(tempBuildDir.resolve("libs"), StandardCopyOption.REPLACE_EXISTING));

        // Handle entryPoint
        String entryPoint =
            "ENTRYPOINT java ${agents}$JVM_OPTIONS -cp @/app/classpath.txt ${mainClass} $PROGRAM_ARGS"
                    .replace("${mainClass}", mainClass)
                    .replace("${agents}", agentInstructions);
        result.add(entryPoint);

        return result;
    }

    /**
     * Returns a formatted string that contains information about this Docker build.
     */
    public String info(String imageName) {

        StringBuilder sb = new StringBuilder(super.info(imageName));

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

        return sb.toString();
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

    private String createClasspathArs(List<Path> sanitizedLibs) {
        StringBuilder classpathValue = new StringBuilder();
        if (classes != null && classes.containFiles()) {
            classpathValue.append("/app/classes");
        }
        sanitizedLibs.forEach(path -> classpathValue.append(":/app/libs/" + path.getFileName()));
        return classpathValue.toString();

    }


    private Map<Object, Path> copyAgents(String imageName) {
        Map<Object, Path> result = new HashMap<>();
        Path tempBuildDir = getTempBuildDir(imageName);
        Path agentDir = tempBuildDir.resolve("agents");
        if (!agents.isEmpty()) {
            JkUtilsPath.createDirectories(agentDir);
        }
        for (Object file : agents.keySet()) {
            final Path agentJar;
            if (file instanceof Path) {
                agentJar = (Path) file;
            } else {
                JkCoordinate coordinate = JkCoordinate.of(file.toString());
                JkCoordinateFileProxy fileProxy = JkCoordinateFileProxy.of(repos, coordinate);
                agentJar = fileProxy.get();
            }
            Path dest = agentDir.resolve(agentJar.getFileName());
            result.put(file, dest);
            JkUtilsPath.copy(agentJar, dest, StandardCopyOption.REPLACE_EXISTING);
        }
        return result;
    }

    private String createAgentOptions(Map<Object, Path> buildDirMap) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Object, String> entry : agents.entrySet()) {
            Path buildDirFile = buildDirMap.get(entry.getKey());
            String agentPath =  "/app/agents/" + buildDirFile.getFileName();
            sb.append("-javaagent:" + agentPath);
            if (!JkUtilsString.isBlank(entry.getValue())) {
                sb.append("=" + entry.getValue());
            }
            sb.append(" ");
        }
        return sb.toString();
    }

    private String createAgentCopyInstruction(Map<Object, Path> buildDirMap) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Object, String> entry : this.agents.entrySet()) {
            Path buildDirFile = buildDirMap.get(entry.getKey());
            String from =  "agents/" + buildDirFile.getFileName();
            String to = "/app/agents/" + buildDirFile.getFileName();
            sb.append("COPY " + from + " " + to + "\n");
        }
        return sb.toString();
    }
}



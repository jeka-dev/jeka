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
 *
 */
public class JkDockerJvmBuild extends JkDockerBuild {

    public static final String BASE_IMAGE = "eclipse-temurin:21.0.2_13-jdk-jammy";

    private static final Predicate<Path> CHANGING_LIB = path -> path.toString().endsWith("-SNAPSHOT.jar");

    private static final JkPathMatcher CLASS_MATCHER = JkPathMatcher.of("**/*.class", "*.class");

    private JkPathTree classes;

    private List<Path> classpath = Collections.emptyList();

    private String mainClass;

    protected JkRepoSet repos = JkRepo.ofMavenCentral().toSet();

    private final Map<Object, String> agents = new HashMap<>();

    private JkDockerJvmBuild() {
        this.setBaseImage(BASE_IMAGE);
        this.nonRootSteps.add(this::enhance);
        this.rootSteps.addNonRootMkdirs("/app", "/workdir");
        this.rootSteps.add("WORKDIR /workdir");
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

    @Override
    public void buildImage(Path contextDir, String imageName) {
        super.buildImage(contextDir, imageName);
        JkLog.info("Pass extra JVM options : Use '-e JAVA_TOOL_OPTIONS=...' option");
        JkLog.info("Pass program arguments : Add arguments to the end of the command line");
        JkLog.info("Map host current dir with container working dir : Use '-v $PWD:/workdir' option");
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
        JkUtilsAssert.state(mainClass != null, "No main class has been defined or found on this project. Please, set the @project.pack.mainClass property.");
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
     * Sets the Maven repository used for downloading agent jars. Initial value is
     * Maven central.
     */
    public final JkDockerBuild setDownloadMavenRepos(JkRepoSet repos) {
        this.repos = repos;
        return this;
    }

    private void enhance(Context context) {

        // Handle Java layers
        List<Path> sanitizedLibs = sanitizedLibs();

        // -- Create non-snapshot layer
        Path libs = context.dir.resolve("libs");
        JkUtilsPath.createDirectories(libs);
        sanitizedLibs.stream()
                .filter(path -> !CHANGING_LIB.test(path))
                .map(JkPathFile::of)
                .forEach(file -> file.copyToDir(libs, StandardCopyOption.REPLACE_EXISTING));
        context.add("COPY libs /app/libs");

        // -- Create snapshot layer
        Path snapshotLibs = context.dir.resolve("snapshot-libs");
        JkUtilsPath.createDirectories(snapshotLibs);
        sanitizedLibs.stream()
                .filter(CHANGING_LIB)
                .map(JkPathFile::of)
                .forEach(file -> file.copyToDir(snapshotLibs, StandardCopyOption.REPLACE_EXISTING));
        context.add("COPY snapshot-libs /app/snapshot-libs");

        // -- Add classpath.txt layer
        Path classpathTxt = context.dir.resolve("classpath.txt");
        String classpathTxtContent = createClasspathArgs(sanitizedLibs);
        JkPathFile.of(classpathTxt).deleteIfExist().write(classpathTxtContent);
        context.add("COPY classpath.txt /app/classpath.txt");

        // -- add resource layer
        Path resources = context.dir.resolve("resources");
        JkUtilsPath.createDirectories(resources);
        resourceFiles().copyTo(resources, StandardCopyOption.REPLACE_EXISTING);
        context.add("COPY resources /app/resources");

        // -- add class layer
        Path classes = context.dir.resolve("classes");
        JkUtilsPath.createDirectories(classes);
        classFiles().copyTo(classes, StandardCopyOption.REPLACE_EXISTING);
        context.add("COPY classes /app/classes");

        // Handle agents
        Map<Object, Path> agentBuildDirMap = copyAgents(context.dir);
        String agentCopy = createAgentCopyInstruction(agentBuildDirMap);
        String agentInstructions = createAgentOptions(agentBuildDirMap);
        if (!agentInstructions.trim().isEmpty() && !agentInstructions.endsWith(" ")) {
            agentInstructions = agentInstructions + " ";
        }
        context.add(agentCopy);

        // Handle entryPoint
        List<String> args = new LinkedList<>();
        args.add("java");
        args.addAll(Arrays.asList(JkUtilsString.parseCommandline(agentInstructions)));
        args.add("-cp");
        args.add("@/app/classpath.txt");
        args.add(mainClass);
        String argLine = toArrayQuotedString(args);
        context.add("ENTRYPOINT " + argLine);
        context.add("CMD []");

    }

    /**
     * Returns a formatted string that contains information about this Docker build.
     */
    public String info(String imageName) {

        StringBuilder sb = new StringBuilder();
        sb.append("Image Name : " + imageName + "\n");
        sb.append("Dockerfile :\n");
        sb.append("-------------------------------------------------\n");
        sb.append(render()).append("\n");
        sb.append("-------------------------------------------------\n");

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

    private String createClasspathArgs(List<Path> sanitizedLibs) {
        StringBuilder classpathValue = new StringBuilder();
        if (classes != null && classes.containFiles()) {
            classpathValue.append("/app/classes:/app/resources");
        }
        sanitizedLibs.forEach(path -> classpathValue.append(":/app/libs/" + path.getFileName()));
        return classpathValue.toString();

    }


    private Map<Object, Path> copyAgents(Path contextDir) {
        Map<Object, Path> result = new HashMap<>();
        Path agentDir = contextDir.resolve("agents");
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



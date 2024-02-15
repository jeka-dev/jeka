package dev.jeka.core.api.tooling.docker;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkConstants;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Experimental
 * Docker Builder assistant for creating Docker images running Java programs.
 */
public class JkDockerBuild {

    private static final String BASE_IMAGE = "eclipse-temurin:21.0.1_12-jdk-jammy";

    private static final Predicate<Path> CHANGING_LIB = path -> path.toString().endsWith("-SNAPSHOT.jar");

    private static final String EXTRA_FILE_DIR = "extra-files";

    private static final JkPathMatcher CLASS_MATCHER = JkPathMatcher.of("**/*.class", "*.class");

    private JkRepoSet repos = JkRepo.ofMavenCentral().toSet();

    private String baseImage = BASE_IMAGE;

    private JkPathTree classes;

    private List<Path> classpath = Collections.emptyList();

    private String mainClass;

    // key is destination within image filesystem, value is path in host filesystem
    private final Map<String, Path> extraFiles = new HashMap<>();

    private final Map<Object, String> agents = new HashMap<>();

    private final Path tempBuildDir = Paths.get(JkConstants.OUTPUT_PATH).resolve("docker-build");

    private final List<String> extraBuildSteps = new LinkedList<>();

    private final List<Integer> exposedPorts = new LinkedList<>();

    private JkDockerBuild() {
    }

    /**
     * Creates a {@link JkDockerBuild instannce.}
     */
    public static JkDockerBuild of() {
        return new JkDockerBuild();
    }

    /**
     * Creates a JkDockerBuild instance for the specified JkProject.
     */
    public static JkDockerBuild of(JkProject project) {
        return of().adaptTo(project);
    }

    /**
     * Adapts this JkDockerBuild instance to build the specified JkProject.
     */
    public JkDockerBuild adaptTo(JkProject project) {
        String mainClass = project.packaging.getMainClass();
        JkUtilsAssert.state(mainClass != null, "No main class has been defined or found on this project.");
        return this
                .setClasses(JkPathTree.of(project.compilation.layout.resolveClassDir()))
                .setClasspath(project.packaging.resolveRuntimeDependenciesAsFiles())
                .setMainClass(mainClass);
    }

    /**
     * Sets the base image name to use a s the base image in the docker file.
     */
    public JkDockerBuild setBaseImage(String baseImage) {
        this.baseImage = baseImage;
        return this;
    }

    /**
     * Sets the compiled Java classes that constitute the Java program to be executed.
     */
    public JkDockerBuild setClasses(JkPathTree classes) {
        this.classes = classes;
        return this;
    }

    /**
     * Sets the jar files that constitute the classpath of the program to be executed.
     */
    public JkDockerBuild setClasspath(List<Path> classpath) {
        this.classpath = classpath;
        return this;
    }

    /**
     * Specifies the main class to run to execute the Program.
     */
    public JkDockerBuild setMainClass(String mainClass) {
        this.mainClass = mainClass;
        return this;
    }

    /**
     * Adds a the specified file at the specified location in the image to construct.
     * @param file The path of the file in the host file system
     * @param destination The path of the file in the image.
     */
    public JkDockerBuild addExtraFile(Path file, String destination) {
        this.extraFiles.put(destination, file);
        return this;
    }

    /**
     * Adds the specified agent to the running JVM
     * @param file The agent jar file in the host system
     * @param agentOptions The agent options that will be prepended to '-javaagent:' option
     */
    public JkDockerBuild addAgent(Path file, String agentOptions) {
        this.agents.put(file, agentOptions);
        return this;
    }

    /**
     * Adds the specified agent to the running JVM
     * @param agentCoordinate The agent maven coordinate to download agent.
     * @param agentOptions The agent options that will be prepended to '-javaagent:' option
     */
    public JkDockerBuild addAgent(@JkDepSuggest String agentCoordinate, String agentOptions) {
        this.agents.put(agentCoordinate, agentOptions);
        return this;
    }

    /**
     * Sets the Maven repository used for downloading agent jars. Initial value is
     * Maven central.
     */
    public JkDockerBuild setDownloadMavenRepos(JkRepoSet repos) {
        this.repos = repos;
        return this;
    }

    /**
     * Adds a docker build statement to be executed at the start of Docker build.
     */
    public JkDockerBuild addExtraBuildStep(String statement) {
        this.extraBuildSteps.add(statement);
        return this;
    }

    /**
     * Adds a port to be exposed by the container.
     */
    public JkDockerBuild setExposedPorts(Integer ...ports) {
        this.exposedPorts.clear();
        this.exposedPorts.addAll(Arrays.asList(ports));
        return this;
    }

    /**
     * Returns an unmodifiable list of exposed ports in the Docker image.
     */
    public List<Integer> getExposedPorts() {
        return Collections.unmodifiableList(exposedPorts);
    }

    /**
     * Builds the docker image with the specified image name.
     * @param imageName The name of the image to be build. It may contains or not a tag name.
     */
    public void buildImage(String imageName) {
        JkDocker.assertPresent();

        JkUtilsAssert.state(!JkUtilsString.isBlank(mainClass), "Class containing 'main' method is not set.");

        JkPathTree.of(tempBuildDir).deleteContent();

        List<Path> sanitizedLibs = sanitizedLibs();

        // Handle extra build steps
        String extraStatements = extraBuildSteps.stream()
                .reduce("", (init, step) -> init + step + "\n" );

        // Handle extra files
        Map<Path, Path> originalToBuildDirFileMap = copyExtraFilesToBuildDir();
        String extraFileCopy = createCopyInstructions(originalToBuildDirFileMap);

        // Handle agents
        Map<Object, Path> agentBuildDirMap = copyAgents();
        String agentCopy = createAgentCopyInstruction(agentBuildDirMap);
        String agentInstructions = createAgentOptions(agentBuildDirMap);

        // Handle exposed ports
        String exposedPortStatements = exposedPorts.isEmpty() ? "" :
                "EXPOSE " + exposedPorts.stream().map(i -> Integer.toString(i))
                        .collect(Collectors.joining(" ")) + "\n";

        String dockerBuildTemplate =
                "FROM ${baseImage}\n" +
                exposedPortStatements +
                extraStatements +
                "WORKDIR /app\n" +
                extraFileCopy +
                agentCopy +
                "COPY libs /app/libs\n" +
                "COPY snapshot-libs /app/libs\n" +
                "COPY classpath.txt /app/classpath.txt\n" +
                "COPY resources /app/classes\n" +
                "COPY classes /app/classes\n" +
                "ENTRYPOINT java ${agents}$JVM_OPTIONS -cp @/app/classpath.txt ${mainClass} $PROGRAM_ARGS";
        String dockerBuild = dockerBuildTemplate
                .replace("${baseImage}", baseImage)
                .replace("${mainClass}", mainClass)
                .replace("${agents}", agentInstructions);
        JkPathFile.of(tempBuildDir.resolve("Dockerfile")).write(dockerBuild);

        JkUtilsPath.createDirectories(tempBuildDir.resolve("libs"));
        JkUtilsPath.createDirectories(tempBuildDir.resolve("snapshot-libs"));
        JkUtilsPath.createDirectories(tempBuildDir.resolve("resources"));
        JkUtilsPath.createDirectories(tempBuildDir.resolve("classes"));

        // Copy classes to docker-build/classes
        classFiles().copyTo(tempBuildDir.resolve("classes"));

        // Copy resources to docker-build/resources
        resourceFiles().copyTo(tempBuildDir.resolve("resources"));

        // Copy classpath.txt
        String classpathTxt = createClasspathArs(sanitizedLibs);
        JkPathFile.of(tempBuildDir.resolve("classpath.txt")).write(classpathTxt);

        // Copy Snapshot libs
        sanitizedLibs.stream()
                .filter(CHANGING_LIB)
                .map(JkPathFile::of)
                .forEach(file -> file.copyToDir(tempBuildDir.resolve("libs")));

        // Copy libs
        sanitizedLibs.stream()
                .filter(path -> !CHANGING_LIB.test(path))
                .map(JkPathFile::of)
                .forEach(file -> file.copyToDir(tempBuildDir.resolve("libs")));

        // Build image using Docker daemon
        JkProcess.of("docker", "build", "-t", imageName, "./" + tempBuildDir).setInheritIO(true)
                .setLogCommand(true).exec();

        String portMapping = portMappingArgs();
        JkLog.info("Run docker image : docker run -it --rm %s%s", portMapping, imageName);
    }

    /**
     * Returns a formatted string that contains information about this Docker build.
     */
    public String info() {

        StringBuilder sb = new StringBuilder();
        sb.append("Base Image Name   : " + this.baseImage).append("\n");

        sb.append("Exposed Ports     : " + this.exposedPorts).append("\n");

        sb.append("Extra Build Steps : ").append("\n");
        this.extraBuildSteps.forEach(item -> sb.append("  " + item).append("\n"));

        sb.append("Extra Files       :").append("\n");
        this.extraFiles.entrySet().forEach(entry
                -> sb.append("  " + entry.getValue()  + " -> " + entry.getKey()+ "\n"));

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

    /**
     * Returns a string representation of the command line arguments for port mapping in the Docker image.
     */
    public String portMappingArgs() {
        String portMapping = "";
        if (!this.exposedPorts.isEmpty()) {
            portMapping = this.exposedPorts.stream()
                    .map(Object::toString)
                    .reduce("-p ", (init, port) -> init  + port + ":" + port + " ");
        }
        return portMapping;
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

    private Map<Path, Path> copyExtraFilesToBuildDir() {
        Map<Path, Path> result = new HashMap<>();
        Path extraFileDir = tempBuildDir.resolve(EXTRA_FILE_DIR);
        if (!extraFiles.isEmpty()) {
            JkUtilsPath.createDirectories(extraFileDir);
        }
        for (Path file : extraFiles.values()) {
            Path dest = extraFileDir.resolve(file.getFileName());
            if (Files.exists(dest)) {
                dest = extraFileDir.resolve(file.getFileName() + "-" + UUID.randomUUID());
            }
            result.put(file, dest);
            JkUtilsPath.copy(file, dest);
        }
        return result;
    }

    private String createCopyInstructions(Map<Path, Path> buildDirMap) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Path> entry : extraFiles.entrySet()) {
            Path buildDirFile = buildDirMap.get(entry.getValue());
            String from =  EXTRA_FILE_DIR + "/" + buildDirFile.getFileName();
            String to = entry.getKey();
            sb.append("COPY " + from + " " + to + "\n");
        }
        return sb.toString();
    }

    private Map<Object, Path> copyAgents() {
        Map<Object, Path> result = new HashMap<>();
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
            JkUtilsPath.copy(agentJar, dest);
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



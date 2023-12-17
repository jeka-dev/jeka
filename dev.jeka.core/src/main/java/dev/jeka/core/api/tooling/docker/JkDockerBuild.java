package dev.jeka.core.api.tooling.docker;

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.tool.JkConstants;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Experimental
 *
 * Docker Builder assistant for creating images for Java programs.
 */
public class JkDockerBuild {

    private static final Predicate<Path> CHANGING_LIB = path -> path.toString().endsWith("-SNAPSHOT.jar");

    private static final Predicate<Path> IS_CLASS = path -> path.toString().endsWith(".class");

    private String baseImage = "eclipse-temurin:17-jdk-jammy";

    private JkPathTree classes;

    private List<Path> classpath;

    private String mainClass;

    private final Path tempBuildDir = Paths.get(JkConstants.OUTPUT_PATH).resolve("docker-build");

    private JkDockerBuild() {
    }

    public static JkDockerBuild of() {
        return new JkDockerBuild();
    }

    public JkDockerBuild setBaseImage(String baseImage) {
        this.baseImage = baseImage;
        return this;
    }

    public JkDockerBuild setClasses(JkPathTree classes) {
        this.classes = classes;
        return this;
    }

    public JkDockerBuild setClasspath(List<Path> classpath) {
        this.classpath = classpath;
        return this;
    }

    public JkDockerBuild setMainClass(String mainClass) {
        this.mainClass = mainClass;
        return this;
    }

    public void buildImage(String imageRepository) {
        JkPathTree.of(tempBuildDir).deleteContent();

        List<Path> sanitizedLibs = classpath.stream()
                .filter(path -> !Files.isDirectory(path))
                .collect(Collectors.toList());

        String dockerBuildTemplate =
                "FROM ${baseImage}\n" +
                "WORKDIR /app\n" +
                "COPY libs /app/libs\n" +
                "COPY snapshot-libs /app/libs\n" +
                "COPY classpath.txt /app/classpath.txt\n" +
                "COPY resources /app/classes\n" +
                "COPY classes /app/classes\n" +
                "ENTRYPOINT java $JVM_OPTIONS -cp @/app/classpath.txt ${mainClass} $PROGRAM_ARGS";
        String dockerBuild = dockerBuildTemplate
                .replace("${baseImage}", baseImage)
                .replace("${mainClass}", mainClass);
        JkPathFile.of(tempBuildDir.resolve("Dockerfile")).write(dockerBuild);

        JkUtilsPath.createDirectories(tempBuildDir.resolve("libs"));
        JkUtilsPath.createDirectories(tempBuildDir.resolve("snapshot-libs"));
        JkUtilsPath.createDirectories(tempBuildDir.resolve("resources"));
        JkUtilsPath.createDirectories(tempBuildDir.resolve("classes"));

        // Copy classes to docker-build/classes
        classes.andMatching("**/*.class", "*.class").copyTo(tempBuildDir.resolve("classes"));

        // Copy resources to docker-build/resources
        //classes.andMatching(false, "**/*.class", "*.class").copyTo(tempBuildDir.resolve("resources"));

        // Copy classpath.txt
        String classpathTxt = createClasspathArs(sanitizedLibs);
        JkPathFile.of(tempBuildDir.resolve("classpath.txt")).write(classpathTxt);

        // Copy Snapshot libs
        sanitizedLibs.stream()
                .filter(path -> CHANGING_LIB.test(path))
                .map(JkPathFile::of)
                .forEach(file -> file.copyToDir(tempBuildDir.resolve("libs")));

        // Copy libs
        sanitizedLibs.stream()
                .filter(path -> !CHANGING_LIB.test(path))
                .map(JkPathFile::of)
                .forEach(file -> file.copyToDir(tempBuildDir.resolve("libs")));

        // Build image using Docker daemon
        JkProcess.of("docker", "build", "-t", imageRepository, "./" + tempBuildDir).setInheritIO(true)
                .setLogCommand(true).exec();
    }

    private String createClasspathArs(List<Path> sanitizedLibs) {
        StringBuilder classpathValue = new StringBuilder();
        if (classes != null && classes.containFiles()) {
            classpathValue.append("/app/classes");
        }
        sanitizedLibs.forEach(path -> classpathValue.append(":/app/libs/" + path.getFileName()));
        return classpathValue.toString();

    }
}

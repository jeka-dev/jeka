package dev.jeka.plugins.springboot;

import dev.jeka.core.api.depmanagement.JkCoordinateFileProxy;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkUrlClassLoader;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsString;

import java.nio.file.Path;
import java.util.List;

public class JkSpringbootJars {

     static final String SPRINGBOOT_APPLICATION_ANNOTATION_NAME =
            "org.springframework.boot.autoconfigure.SpringBootApplication";

    static final String LOADER_COORDINATE = "org.springframework.boot:spring-boot-loader:";

    static final String DEFAULT_SPRINGBOOT_VERSION = "3.2.0";

    /**
     * Creates a bootable jar from class files and libraries to include in the bootable jar.
     */
    public static void createBootJar(JkPathTree<?> classTree, List<Path> libsToInclude, JkRepoSet downloadRepo,
                                     Path targetJar) {
        JkUtilsAssert.argument(classTree.exists(), "class dir not found " + classTree.getRoot());
        String mainClassName = findMainClassName(classTree.getRoot());
        JkCoordinateFileProxy loaderProxy = JkCoordinateFileProxy.of(downloadRepo,
                LOADER_COORDINATE + DEFAULT_SPRINGBOOT_VERSION);

        SpringbootPacker.of(libsToInclude, loaderProxy.get(), mainClassName)
                .makeExecJar(classTree, targetJar);
    }

    private static String findMainClassName(Iterable<Path> jarOrFolder) {
        JkClassLoader classLoader = JkUrlClassLoader.of(jarOrFolder, ClassLoader.getSystemClassLoader().getParent())
                .toJkClassLoader();
        List<String> mainClasses = classLoader.findClassesHavingMainMethod();
        List<String> classWithSpringbootAppAnnotation = classLoader.findClassesMatchingAnnotations(
                annotationNames -> annotationNames.contains(SPRINGBOOT_APPLICATION_ANNOTATION_NAME));
        for (String name : mainClasses) {
            if (classWithSpringbootAppAnnotation.contains(name)) {
                return name;
            }
        }

        // Kotlin adds a special [mainClass]Kt class to host main method
        for (String name : mainClasses) {
            if (name.endsWith("Kt")) {
                String originalName = JkUtilsString.substringBeforeLast(name, "Kt");
                if (classWithSpringbootAppAnnotation.contains(originalName)) {
                    return name;
                }
            }
        }
        throw new IllegalStateException("No class annotated with @SpringBootApplication found.");
    }

}
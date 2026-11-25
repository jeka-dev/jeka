package e2e;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Shading can't be tested in unit tests, cause of the classpath.
 */
class ShadeTest {

    @TempDir
    Path tempDir;

    @BeforeAll
    static void beforeAll() {
        JkLog.setDecorator(JkLog.Style.INDENT);
    }

    @Test
    void shadeJava25Classes_ok() throws Exception {
        Path dir = tempDir.toRealPath();
        Files.writeString(dir.resolve("jeka.properties"), """
                @project=on
                @project.moduleId=org.example:shaded
                @project.pack.jarType=SHADE
                @project.pack.mainClass=app.Main
                """);
        Files.writeString(dir.resolve("jeka.project.deps"), """
                [compile]
                com.google.code.gson:gson:2.11.0
                """);
        Path source = dir.resolve("src/main/java/app/Main.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, """
                package app;
                import com.google.gson.Gson;
                public class Main {
                    public static void main(String[] args) {
                        System.out.println(new Gson().toJson(new int[] {1, 2}) + Gson.class.getName());
                    }
                }
                """);

        new JekaCmdLineExecutor().runWithDistribJekaShell(dir, "project: pack");

        Path jar = dir.resolve("jeka-output/org.example.shaded.jar");
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            assertNotNull(jarFile.getEntry("app/_shaded/com/google/gson/Gson.class"), "Dependency is not relocated");
            assertNull(jarFile.getEntry("com/google/gson/Gson.class"), "Dependency is not relocated");
            byte[] mainClass = jarFile.getInputStream(jarFile.getEntry("app/Main.class")).readAllBytes();
            assertEquals(69, mainClass[7], "Main class should be compiled for Java 25");
        }
    }

    @Test
    void shadeServiceFiles_relocatedAndMerged() throws Exception {
        Path dir = tempDir.toRealPath();
        Files.writeString(dir.resolve("jeka.properties"), """
                @project=on
                @project.moduleId=org.example:shaded
                @project.javaVersion=21
                @project.pack.jarType=SHADE
                @project.pack.mainClass=app.Main
                """);

        // Both dependencies provide a org.slf4j.spi.SLF4JServiceProvider
        Files.writeString(dir.resolve("jeka.project.deps"), """
                [compile]
                org.slf4j:slf4j-api:2.0.13
                org.slf4j:slf4j-simple:2.0.13
                org.slf4j:slf4j-nop:2.0.13
                """);
        Path source = dir.resolve("src/main/java/app/Main.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, """
                package app;
                import org.slf4j.spi.SLF4JServiceProvider;
                import java.util.ServiceLoader;
                public class Main {
                    public static void main(String[] args) {
                        for (SLF4JServiceProvider provider : ServiceLoader.load(SLF4JServiceProvider.class)) {
                            System.out.println("found:" + provider.getClass().getName());
                        }
                        ServiceLoader.load(Runnable.class).forEach(Runnable::run);
                    }
                }
                """);

        // JDK service implemented by the application: nothing must be renamed
        Files.writeString(dir.resolve("src/main/java/app/Task.java"), """
                package app;
                public class Task implements Runnable {
                    public void run() {
                        System.out.println("found:" + getClass().getName());
                    }
                }
                """);
        Path services = dir.resolve("src/main/resources/META-INF/services");
        Files.createDirectories(services);
        Files.writeString(services.resolve("java.lang.Runnable"), "app.Task\n");

        new JekaCmdLineExecutor().runWithDistribJekaShell(dir, "project: pack");

        Path jar = dir.resolve("jeka-output/org.example.shaded.jar");
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            assertNull(jarFile.getEntry("META-INF/services/org.slf4j.spi.SLF4JServiceProvider"),
                    "Service file is not relocated");
        }
        String output = JkProcess.of(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                        "-jar", jar.toString())
                .setCollectStdout(true)
                .exec().getStdoutAsString();
        assertTrue(output.contains("found:app._shaded.org.slf4j.simple.SimpleServiceProvider"), output);
        assertTrue(output.contains("found:app._shaded.org.slf4j.nop.NOPServiceProvider"), output);
        assertTrue(output.contains("found:app.Task"), output);
    }

    @Test
    void shade_onlyRelocatesDependencyPackages() throws Exception {
        Path dir = tempDir.toRealPath();
        Files.writeString(dir.resolve("jeka.properties"), """
                @project=on
                @project.moduleId=org.example:shaded
                @project.javaVersion=21
                @project.pack.jarType=SHADE
                @project.pack.mainClass=org.example.Main
                """);

        // Logback refers to org.xml.sax, and jsr305 contains javax.annotation
        Files.writeString(dir.resolve("jeka.project.deps"), """
                [compile]
                ch.qos.logback:logback-classic:1.5.18
                com.google.code.findbugs:jsr305:3.0.2
                """);
        Path sources = dir.resolve("src/main/java/org/example");
        Files.createDirectories(sources.resolve("sub"));
        Files.writeString(sources.resolve("Main.java"), """
                package org.example;
                public class Main {
                    public static void main(String[] args) {
                        org.slf4j.LoggerFactory.getLogger(Main.class).info("logged");
                        System.out.println("found:" + org.w3c.dom.Node.class.getName());
                        System.out.println("found:" + javax.xml.parsers.SAXParserFactory.class.getName());
                        System.out.println("found:" + javax.annotation.Nonnull.class.getName());
                        System.out.println("found:" + org.example.sub.Other.class.getName());
                    }
                }
                """);
        Files.writeString(sources.resolve("sub/Other.java"), """
                package org.example.sub;
                public class Other {
                }
                """);
        // Class names in text files are not relocated
        Path resources = dir.resolve("src/main/resources");
        Files.createDirectories(resources);
        Files.writeString(resources.resolve("logback.xml"), """
                <configuration>
                  <appender name="C" class="org.example._shaded.ch.qos.logback.core.ConsoleAppender">
                    <encoder><pattern>logback:%msg%n</pattern></encoder>
                  </appender>
                  <root level="info"><appender-ref ref="C"/></root>
                </configuration>
                """);

        new JekaCmdLineExecutor().runWithDistribJekaShell(dir, "project: pack");

        Path jar = dir.resolve("jeka-output/org.example.shaded.jar");
        String output = JkProcess.of(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                        "-jar", jar.toString())
                .setCollectStdout(true)
                .exec().getStdoutAsString();
        assertTrue(output.contains("found:org.w3c.dom.Node"), output);
        assertTrue(output.contains("found:javax.xml.parsers.SAXParserFactory"), output);
        assertTrue(output.contains("found:org.example._shaded.javax.annotation.Nonnull"), output);
        assertTrue(output.contains("found:org.example.sub.Other"), output);
        assertTrue(output.contains("logback:logged"), output);
    }

}

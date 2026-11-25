package e2e;

import dev.jeka.core.api.system.JkLog;
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

}

package dev.jeka.core.tool.builtins.project;

import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.tool.KBeanTestLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BundleKBeanTest {

    @TempDir
    Path tempDir;

    @Test
    void pack_customJreNoDeclaredModules_modulesAreComputed() throws Exception {
        Path baseDir = tempDir.toRealPath();
        String release = packAndReadRelease(baseDir, "");
        assertTrue(release.contains("java.logging"), release);
        assertFalse(release.contains("java.sql"), release);
    }

    @Test
    void pack_customJreModulesSetByUser_modulesAreKept() throws Exception {
        Path baseDir = tempDir.toRealPath();
        String release = packAndReadRelease(baseDir,
                "@bundle.jlink.options.all.--add-modules=java.base,java.xml\n");
        assertTrue(release.contains("java.xml"), release);
        assertFalse(release.contains("java.logging"), release);
    }

    @Test
    void jdkHome_jdkSetInProperties_isUsed() throws Exception {
        Path baseDir = tempDir.toRealPath();
        Path jdkHome = Files.createDirectories(baseDir.resolve("my-jdk"));
        String javaVersion = JkJavaVersion.ofCurrent().toString();
        write(baseDir.resolve("jeka.properties"), "@project=on\n@bundle=on\njeka.jdk." + javaVersion + "="
                + jdkHome.toString().replace("\\", "/") + "\n");
        assertEquals(jdkHome, KBeanTestLoader.load(baseDir, BundleKBean.class).jdkHome());
    }

    private static String packAndReadRelease(Path baseDir, String extraProps) throws Exception {
        write(baseDir.resolve("jeka.properties"),
                "@project=on\n@project.pack.mainClass=app.Main\n@bundle=on\n@bundle.customJre=true\n"
                        + "@bundle.jpackage.options.all.--type=app-image\n" + extraProps);
        write(baseDir.resolve("src/main/java/app/Main.java"),
                "package app; public class Main { public static void main(String[] args) { "
                        + "java.util.logging.Logger.getGlobal(); } }");
        KBeanTestLoader.load(baseDir, BundleKBean.class).pack();
        return new String(Files.readAllBytes(baseDir.resolve("jeka-output/jlink/custom-jre/release")));
    }

    private static void write(Path file, String content) throws Exception {
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes());
    }

}

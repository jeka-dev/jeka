package dev.jeka.core.api.java;

import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.utils.JkUtilsPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

import static org.junit.jupiter.api.Assertions.*;

class JkJarPackerManifestTest {

    @TempDir
    Path dir;

    private Path classDir;

    private Path jar;

    @BeforeEach
    void setUp() {
        classDir = dir.resolve("classes");
        jar = dir.resolve("out.jar");
        JkUtilsPath.createDirectories(classDir);
        JkUtilsPath.write(classDir.resolve("a.txt"), "a".getBytes());
    }

    @Test
    void makeFatJar_afterMakeJar_hasItsOwnManifest() throws IOException {
        packer("1").makeJar(jar);
        assertEquals("1", versionInJar());

        packer("2").makeFatJar(jar, Collections.emptyList(), JkPathMatcher.of());
        assertEquals("2", versionInJar());
    }

    @Test
    void makeJar_doesNotChangeClassDir() {
        packer("1").makeJar(jar);
        assertEquals(1, JkPathTree.of(classDir).count(Integer.MAX_VALUE, true));
    }

    @Test
    void makeJar_onExistingJar_replacesManifest() throws IOException {
        packer("1").makeJar(jar);
        packer("2").makeJar(jar);
        assertEquals("2", versionInJar());
    }

    @Test
    void makeJar_manifestInClassDir_isOverriddenButKeptInClassDir() throws IOException {
        JkManifest.of().addMainAttribute("Implementation-Version", "0").writeToStandardLocation(classDir);
        packer("1").makeJar(jar);
        assertEquals("1", versionInJar());
        assertEquals("0", JkManifest.of().loadFromFile(classDir.resolve(JkManifest.STANDARD_LOCATION))
                .getMainAttribute("Implementation-Version"));
    }

    @Test
    void makeJar_manifestIsReadableAsStream() throws IOException {
        packer("1").makeJar(jar);
        try (JarInputStream is = new JarInputStream(Files.newInputStream(jar))) {
            assertNotNull(is.getManifest());
        }
    }

    @Test
    void makeJar_withoutManifest_keepsTheOneOfClassDir() throws IOException {
        JkManifest.of().addMainAttribute("Implementation-Version", "0").writeToStandardLocation(classDir);
        JkJarPacker.of(classDir).makeJar(jar);
        assertEquals("0", versionInJar());
    }

    @Test
    void makeJarAndFatJar_manifestInClassDir_attributesMergedUnderGenerated() throws IOException {
        JkManifest.of()
                .addMainAttribute("Implementation-Version", "0")
                .addMainAttribute("X-Custom", "kept")
                .writeToStandardLocation(classDir);

        packer("1").makeJar(jar);
        assertEquals("1", versionInJar());
        assertEquals("kept", mainAttributeInJar("X-Custom"));

        packer("2").makeFatJar(jar, Collections.emptyList(), JkPathMatcher.of());
        assertEquals("2", versionInJar());
        assertEquals("kept", mainAttributeInJar("X-Custom"));
    }

    @Test
    void makeFatJar_manifestIsFirstEntry() throws IOException {
        JkManifest.of().addMainAttribute("X-Custom", "kept").writeToStandardLocation(classDir);
        packer("1").makeFatJar(jar, Collections.emptyList(), JkPathMatcher.of());
        try (JarInputStream is = new JarInputStream(Files.newInputStream(jar))) {
            assertNotNull(is.getManifest());
            assertEquals("1", is.getManifest().getMainAttributes().getValue("Implementation-Version"));
        }
    }

    private String mainAttributeInJar(String name) throws IOException {
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            return jarFile.getManifest().getMainAttributes().getValue(name);
        }
    }

    private JkJarPacker packer(String version) {
        return JkJarPacker.of(classDir)
                .withManifest(JkManifest.of().addMainAttribute("Implementation-Version", version));
    }

    private String versionInJar() throws IOException {
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            assertNotNull(jarFile.getEntry("a.txt"));
            return jarFile.getManifest().getMainAttributes().getValue("Implementation-Version");
        }
    }

}

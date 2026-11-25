package dev.jeka.core.api.java.embedded.shade;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ShadedPackagesTest {

    private final ShadedPackages packages = ShadedPackages.of(
            List.of("org/example/Main.class", "org/example/sub/Other.class", "logback.xml"),
            List.of("org/slf4j/Logger.class",
                    "org/slf4j/messages/en.properties",
                    "org/example/Split.class",
                    "com/acme/tool/Tool.class",
                    "javax/annotation/Nonnull.class",
                    "org/xml/sax/InputSource.class",
                    "static/index.html",
                    "module-info.class",
                    "META-INF/services/org.slf4j.Provider",
                    "META-INF/versions/21/net/mr/Recent.class"));

    @Test
    void roots_onlyFoldersOfDependencyClasses() {
        assertEquals(Set.of("org", "com", "javax", "net"), packages.roots());
    }

    @Test
    void contains_dependencyClassesResourcesAndPackages() {
        assertTrue(packages.contains("org/slf4j/Logger"));
        assertTrue(packages.contains("org/slf4j/Logger.class"));
        assertTrue(packages.contains("/org/slf4j/messages/en.properties"));
        assertTrue(packages.contains("org/slf4j"));
        assertTrue(packages.contains("com/acme/"));
        assertTrue(packages.contains("javax/annotation/Nonnull"));
        assertTrue(packages.contains("net/mr/Recent"));
    }

    @Test
    void contains_jdkPackages_false() {
        assertFalse(packages.contains("org/xml/sax/InputSource"));
        assertFalse(packages.contains("org/w3c/dom/Node"));
        assertFalse(packages.contains("javax/xml/parsers/SAXParserFactory"));
        assertFalse(packages.contains("org"));
        assertFalse(packages.contains("javax"));
    }

    @Test
    void contains_applicationAndAbsentPackages_false() {
        assertFalse(packages.contains("org/example/Main"));
        assertFalse(packages.contains("org/example/Split"));
        assertFalse(packages.contains("org/example/sub/Other"));
        assertFalse(packages.contains("org/absent/Optional"));
        assertFalse(packages.contains("static/index.html"));
        assertFalse(packages.contains("META-INF/services/org.slf4j.Provider"));
    }

    @Test
    void ofJars_readsEntries(@TempDir Path dir) throws IOException {
        Path app = jar(dir.resolve("app.jar"), "org/example/", "org/example/Main.class");
        Path dep1 = jar(dir.resolve("dep1.jar"), "org/", "org/slf4j/", "org/slf4j/Logger.class");
        Path dep2 = jar(dir.resolve("dep2.jar"), "com/acme/tool/Tool.class");
        ShadedPackages result = ShadedPackages.ofJars(app, List.of(dep1, dep2));
        assertEquals(Set.of("org", "com"), result.roots());
        assertTrue(result.contains("org/slf4j/Logger"));
        assertTrue(result.contains("com/acme/tool/Tool"));
        assertFalse(result.contains("org/example/Main"));
    }

    @Test
    void ofJars_missingJar_fails(@TempDir Path dir) {
        Path missing = dir.resolve("missing.jar");
        List<Path> none = List.of();
        assertThrows(UncheckedIOException.class, () -> ShadedPackages.ofJars(missing, none));
    }

    private static Path jar(Path file, String... entries) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(file))) {
            for (String entry : entries) {
                zip.putNextEntry(new ZipEntry(entry));
                zip.closeEntry();
            }
        }
        return file;
    }

}

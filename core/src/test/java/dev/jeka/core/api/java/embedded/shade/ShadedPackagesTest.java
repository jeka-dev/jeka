package dev.jeka.core.api.java.embedded.shade;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

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

}

package dev.jeka.core.api.depmanagement.publication;

import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JkArtifactPublisherTest {

    @TempDir
    Path outputDir;

    @Test
    void makeMissingArtifacts_mainJarMissing_suggestsToPack() {
        JkArtifactPublisher publisher = JkArtifactPublisher.of(JkArtifactLocator.of(outputDir, "my-lib"))
                .putArtifact(JkArtifactId.MAIN_JAR_ARTIFACT_ID, null)
                .putArtifact(JkArtifactId.SOURCES_ARTIFACT_ID, path -> {});

        Exception e = assertThrows(IllegalArgumentException.class, publisher::makeMissingArtifacts);

        assertTrue(e.getMessage().contains("project: pack"), e.getMessage());
        assertTrue(e.getMessage().contains("my-lib.jar"), e.getMessage());
        assertFalse(e.getMessage().contains("Lambda"), e.getMessage());
    }

}

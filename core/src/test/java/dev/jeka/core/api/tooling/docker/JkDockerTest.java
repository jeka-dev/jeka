package dev.jeka.core.api.tooling.docker;

import dev.jeka.core.api.utils.JkUtilsPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class JkDockerTest {

    // Images of this test only, so that cleaning them can't remove other images
    private static final String PREFIX = "jk-test-jkdocker-";

    private final JkDocker docker = JkDocker.of();

    @BeforeEach
    void setUp() {
        assumeTrue(docker.isPresent(), "Docker is not available");
        docker.removeImagesStartingWith(PREFIX);
    }

    @AfterEach
    void tearDown() {
        if (docker.isPresent()) {
            docker.removeImagesStartingWith(PREFIX);
        }
    }

    @Test
    void removeImage_existingImage_removedOnce() throws IOException {
        String imageName = PREFIX + "single";
        buildEmptyImage(imageName);
        assertTrue(docker.hasLocalImage(imageName));
        assertTrue(docker.hasLocalImage(imageName + ":latest"), "Tag 'latest' should be assumed");

        assertTrue(docker.removeImage(imageName));

        assertFalse(docker.hasLocalImage(imageName));
        assertFalse(docker.removeImage(imageName), "Removing a missing image should do nothing");
    }

    @Test
    void removeImagesStartingWith_matchingImages_onlyThoseRemoved() throws IOException {
        buildEmptyImage(PREFIX + "a:1");
        buildEmptyImage(PREFIX + "b");
        String other = "jk-test-jkdockerother-c";
        buildEmptyImage(other);
        try {
            List<String> removed = docker.removeImagesStartingWith(PREFIX);

            assertTrue(removed.contains(PREFIX + "a:1"), removed.toString());
            assertTrue(removed.contains(PREFIX + "b:latest"), removed.toString());
            assertFalse(docker.hasLocalImage(PREFIX + "a:1"));
            assertFalse(docker.hasLocalImage(PREFIX + "b"));
            assertTrue(docker.hasLocalImage(other), "An image without the prefix should be kept");
        } finally {
            docker.removeImage(other);
        }
    }

    @Test
    void blankArguments_rejected() {
        assertThrows(IllegalArgumentException.class, () -> docker.removeImagesStartingWith(" "));
        assertThrows(IllegalArgumentException.class, () -> docker.hasLocalImage(""));
    }

    // An image with no layer, built without network
    private void buildEmptyImage(String imageName) throws IOException {
        Path contextDir = JkUtilsPath.createTempDirectory("jk-test-docker-ctx");
        Files.write(contextDir.resolve("Dockerfile"), "FROM scratch\nLABEL dev.jeka.test=true\n".getBytes());
        JkDocker.of().addParams("build", "-q", "-t", imageName, contextDir.toString())
                .setInheritIO(false)
                .setCollectStdout(true)
                .exec();
    }

}

package dev.jeka.core.api.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JkJarPackerShadeTest {

    @TempDir
    Path dir;

    @Test
    void shade_success_replacesExistingJar() throws IOException {
        Path output = staleJar();
        JkJarPacker.makeShadeJar((main, extras, out) -> write(out, "shaded"), dir.resolve("main.jar"),
                Collections.emptyList(), output);
        assertEquals(List.of("shaded.jar"), fileNames(output.getParent()));
        assertEquals("shaded", new String(Files.readAllBytes(output)));
    }

    @Test
    void shade_success_createsParentDir() throws IOException {
        Path output = dir.resolve("new-dir/shaded.jar");
        JkJarPacker.makeShadeJar((main, extras, out) -> write(out, "shaded"), dir.resolve("main.jar"),
                Collections.emptyList(), output);
        assertEquals(List.of("shaded.jar"), fileNames(output.getParent()));
    }

    @Test
    void shade_failure_leavesNeitherPartialNorStaleJar() throws IOException {
        Path output = staleJar();
        assertThrows(IllegalStateException.class, () -> JkJarPacker.makeShadeJar((main, extras, out) -> {
            write(out, "partial");
            throw new IllegalStateException("shade failure");
        }, dir.resolve("main.jar"), Collections.emptyList(), output));
        assertEquals(List.of(), fileNames(output.getParent()));
    }

    private Path staleJar() throws IOException {
        Path output = dir.resolve("out/shaded.jar");
        Files.createDirectories(output.getParent());
        return Files.write(output, "stale".getBytes());
    }

    private static void write(Path file, String content) {
        try {
            Files.write(file, content.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<String> fileNames(Path dir) throws IOException {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.map(path -> path.getFileName().toString()).collect(Collectors.toList());
        }
    }

}

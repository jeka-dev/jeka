package dev.jeka.core.api.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class JkUtilsIOGobblerTest {

    @TempDir
    Path dir;

    @Test
    @Timeout(60)
    void processAlreadyDead_outputIsCollected() throws Exception {
        Process process = new ProcessBuilder(java(), "--version").start();
        assertEquals(0, process.waitFor());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JkUtilsIO.newStreamGobbler(process, process.getInputStream(), out).join();
        assertFalse(out.toString().trim().isEmpty());
    }

    @Test
    @Timeout(60)
    void nonAsciiOutput_isNotAltered() throws Exception {
        byte[] content = new byte[20_000];  // larger than the buffer
        byte[] text = "é€日本😀\n".getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < content.length; i++) {
            content[i] = text[i % text.length];
        }
        content[0] = (byte) 0xFF;  // not valid in UTF-8
        Path file = dir.resolve("content.txt");
        Files.write(file, content);
        Path source = dir.resolve("Cat.java");
        Files.write(source, ("public class Cat { public static void main(String[] args) throws Exception {"
                + "java.nio.file.Files.copy(java.nio.file.Path.of(args[0]), System.out); System.out.flush(); } }")
                .getBytes());

        Process process = new ProcessBuilder(java(), source.toString(), file.toString()).start();
        ByteArrayOutputStream out1 = new ByteArrayOutputStream();
        ByteArrayOutputStream out2 = new ByteArrayOutputStream();
        JkUtilsIO.newStreamGobbler(process, process.getInputStream(), out1, out2).join();

        assertArrayEquals(content, out1.toByteArray());
        assertArrayEquals(content, out2.toByteArray());
    }

    private static String java() {
        return Paths.get(System.getProperty("java.home"), "bin", "java").toString();
    }

}

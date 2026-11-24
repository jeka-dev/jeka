package dev.jeka.core.api.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.ByteArrayOutputStream;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class JkUtilsIOGobblerTest {

    @Test
    @Timeout(60)
    void processAlreadyDead_outputIsCollected() throws Exception {
        String java = Paths.get(System.getProperty("java.home"), "bin", "java").toString();
        Process process = new ProcessBuilder(java, "--version").start();
        assertEquals(0, process.waitFor());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JkUtilsIO.newStreamGobbler(process, process.getInputStream(), out).join();
        assertFalse(out.toString().trim().isEmpty());
    }

}

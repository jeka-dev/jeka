package dev.jeka.core.api.tooling.maven;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JkMvnTest {

    @Test
    void mvnCmd_noMavenNorWrapper_failsWithClearMessage(@TempDir Path dir) {
        IllegalStateException unix = assertThrows(IllegalStateException.class,
                () -> JkMvn.mvnCmd(dir, false, cmd -> false));
        assertTrue(unix.getMessage().startsWith("This command needs Maven"), unix.getMessage());

        IllegalStateException windows = assertThrows(IllegalStateException.class,
                () -> JkMvn.mvnCmd(dir, true, cmd -> false));
        assertTrue(windows.getMessage().contains(dir.toAbsolutePath().normalize().toString()), windows.getMessage());
    }

    @Test
    void mvnCmd_mavenInPath_isUsed(@TempDir Path dir) {
        assertEquals("mvn", JkMvn.mvnCmd(dir, false, "mvn"::equals));
        assertEquals("mvn.cmd", JkMvn.mvnCmd(dir, true, "mvn.cmd"::equals));
    }

    @Test
    void mvnCmd_wrapperPresent_isPreferred(@TempDir Path dir) throws Exception {
        Files.createFile(dir.resolve("mvnw"));
        Files.createFile(dir.resolve("mvnw.cmd"));
        assertEquals("./mvnw", JkMvn.mvnCmd(dir, false, cmd -> true));
        assertEquals("mvnw.cmd", JkMvn.mvnCmd(dir, true, cmd -> true));
    }

}

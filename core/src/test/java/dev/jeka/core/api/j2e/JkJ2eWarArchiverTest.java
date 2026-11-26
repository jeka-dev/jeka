package dev.jeka.core.api.j2e;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkZipTree;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

class JkJ2eWarArchiverTest {

    @Test
    void generateWarFile_onExistingWar_doesNotKeepOldEntries() throws Exception {
        Path base = Files.createTempDirectory("jk-war");
        Path classDir = base.resolve("classes");
        Files.createDirectories(classDir);
        Path deleted = Files.writeString(classDir.resolve("Deleted.class"), "");
        Files.writeString(classDir.resolve("Kept.class"), "");
        Path war = base.resolve("app.war");
        JkJ2eWarArchiver archiver = JkJ2eWarArchiver.of()
                .setClassDir(classDir)
                .setLibs(Collections.emptyList());
        archiver.generateWarFile(war);

        Files.delete(deleted);
        archiver.generateWarFile(war);

        try (JkZipTree zipTree = JkZipTree.of(war)) {
            Assertions.assertTrue(Files.exists(zipTree.get("WEB-INF/classes/Kept.class")));
            Assertions.assertFalse(Files.exists(zipTree.get("WEB-INF/classes/Deleted.class")));
        }
        JkPathTree.of(base).deleteRoot();
    }

}

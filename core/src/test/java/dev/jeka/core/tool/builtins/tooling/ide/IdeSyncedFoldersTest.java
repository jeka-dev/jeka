package dev.jeka.core.tool.builtins.tooling.ide;

import dev.jeka.core.tool.KBeanTestLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class IdeSyncedFoldersTest {

    @Test
    void markSynced_sameFolder_onlyOncePerKind(@TempDir Path dir) {
        assertTrue(IdeSyncedFolders.markSynced("intellij", dir));
        assertFalse(IdeSyncedFolders.markSynced("intellij", dir.resolve("x/..")));
        assertTrue(IdeSyncedFolders.markSynced("eclipse", dir));
    }

    @Test
    void syncAll_noImlFile_nothingDone(@TempDir Path dir) throws Exception {
        assertDoesNotThrow(() -> KBeanTestLoader.load(dir.toRealPath(), IntellijKBean.class).syncAll());
    }

}

package dev.jeka.core.tool.builtins.tooling.ide;

import dev.jeka.core.tool.KBeanTestLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

class EclipseKBeanTest {

    @Test
    void all_dirsWithoutJekaSrc_areIgnored(@TempDir Path tempDir) throws Exception {
        Path baseDir = tempDir.toRealPath();
        Path module = Files.createDirectories(baseDir.resolve("module/src")).getParent();
        KBeanTestLoader.load(baseDir, EclipseKBean.class).all();
        assertFalse(Files.exists(module.resolve(".classpath")));
        assertFalse(Files.exists(module.resolve(".project")));
    }

}

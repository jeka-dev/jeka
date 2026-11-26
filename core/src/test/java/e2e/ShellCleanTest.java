package e2e;

import dev.jeka.core.api.file.JkPathTree;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

class ShellCleanTest {

    private final JekaCmdLineExecutor executor = new JekaCmdLineExecutor();

    @Test
    void clean_withRemoteBaseDir_cleansBaseDirOutputOnly() throws Exception {
        Path dir = Files.createTempDirectory("jk-shell-clean");
        Path workingDir = Files.createDirectories(dir.resolve("working-dir"));
        Path workingDirOutput = Files.createDirectories(workingDir.resolve("jeka-output"));
        Files.createFile(workingDirOutput.resolve("keep.txt"));
        Path baseDir = Files.createDirectories(dir.resolve("base-dir"));
        Files.writeString(baseDir.resolve("jeka.properties"), "");
        Path baseDirOutput = Files.createDirectories(baseDir.resolve("jeka-output"));
        Files.createFile(baseDirOutput.resolve("delete.txt"));

        executor.runWithDistribJekaShell(workingDir, "-r " + baseDir + " -c --version");

        Assertions.assertTrue(Files.exists(workingDirOutput.resolve("keep.txt")),
                "jeka-output of the working dir should not be deleted");
        Assertions.assertFalse(Files.exists(baseDirOutput.resolve("delete.txt")),
                "jeka-output of the base dir should be deleted");
        JkPathTree.of(dir).deleteRoot();
    }

}

package e2e;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkProcResult;
import dev.jeka.core.api.utils.JkUtilsSystem;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

class ShellDistribRepoTest {

    private final JekaCmdLineExecutor executor = new JekaCmdLineExecutor();

    @Test
    void distribRepo_set_usedToDownloadDistrib() throws Exception {

        // The PowerShell script does not print the download URL
        Assumptions.assumeFalse(JkUtilsSystem.IS_WINDOWS);
        Path baseDir = Files.createTempDirectory("jk-shell-distrib-repo");

        // Nothing listens on port 9: the download fails, after the URL is printed
        Files.writeString(baseDir.resolve("jeka.properties"),
                "jeka.version=0.0.1-distrib-repo-test\njeka.distrib.repo=http://127.0.0.1:9/my-repo\n");

        // The executor sets the distrib location, which prevents any download
        JkProcResult result = executor.prepareJeka(false, baseDir, "--version")
                .setEnv("jeka.distrib.location", "")
                .setFailOnError(false)
                .setCollectStderr(true)
                .setCollectStdout(true)
                .exec();

        String output = result.getStdoutAsString() + result.getStderrAsString();
        Assertions.assertNotEquals(0, result.getExitCode());
        Assertions.assertTrue(output.contains("http://127.0.0.1:9/my-repo/dev/jeka/jeka-core/"), output);
        JkPathTree.of(baseDir).deleteRoot();
    }

}

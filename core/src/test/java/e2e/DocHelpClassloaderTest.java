package e2e;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkProcResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * '--doc' must not load the KBean classes a second time, in another classloader: a static initializer mixing
 * classes of both would fail, as with working-examples/kubernetes.
 */
class DocHelpClassloaderTest {

    private final JekaCmdLineExecutor executor = new JekaCmdLineExecutor();

    @Test
    void doc_enumInitializer_runsInContextClassloader() throws Exception {
        Path baseDir = Files.createTempDirectory("jk-doc-classloader");
        Files.createDirectories(baseDir.resolve("jeka-src"));
        Files.writeString(baseDir.resolve("jeka-src/Build.java"),
                "import dev.jeka.core.tool.KBean;\n"
                + "public class Build extends KBean {\n"
                + "  public enum Mode {\n"
                + "    A, B;\n"
                + "    static {\n"
                + "      if (Mode.class.getClassLoader() != Thread.currentThread().getContextClassLoader()) {\n"
                + "        throw new IllegalStateException(\"Mode loaded by another classloader\");\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "  public Mode mode = Mode.A;\n"
                + "  public void hi() { }\n"
                + "}\n");

        for (String cmdLine : new String[] {"--doc", "build: --doc"}) {
            JkProcResult result = executor.prepareJeka(false, baseDir, cmdLine)
                    .setFailOnError(false)
                    .setCollectStdout(true)
                    .setCollectStderr(true)
                    .exec();
            String output = result.getStdoutAsString() + result.getStderrAsString();
            Assertions.assertEquals(0, result.getExitCode(), cmdLine + ":\n" + output);
            Assertions.assertTrue(output.contains("mode"), cmdLine + ":\n" + output);
        }
        JkPathTree.of(baseDir).deleteRoot();
    }

}

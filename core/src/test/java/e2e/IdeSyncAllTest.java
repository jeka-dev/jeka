package e2e;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkProcResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Run from a parent declaring child bases, 'intellij: syncAll' and 'eclipse: all' process each module once.
 */
class IdeSyncAllTest {

    private final JekaCmdLineExecutor executor = new JekaCmdLineExecutor();

    @Test
    void syncAll_fromParent_eachModuleOnce() throws Exception {
        Path baseDir = Files.createTempDirectory("jk-ide-sync-all").toRealPath();
        Files.writeString(baseDir.resolve("jeka.properties"), "_jeka.child-bases=a, b\n");
        for (String module : new String[] {"a", "b"}) {
            Files.createDirectories(baseDir.resolve(module + "/jeka-src"));
            Files.writeString(baseDir.resolve(module + "/jeka.properties"), "");
            Files.writeString(baseDir.resolve(module + "/" + module + ".iml"), "");
        }

        String intellijOutput = run(baseDir, "intellij: syncAll");
        assertOnce(intellijOutput, "Generate iml file on '" + baseDir.resolve("a") + "'");
        assertOnce(intellijOutput, "Generate iml file on '" + baseDir.resolve("b") + "'");

        String eclipseOutput = run(baseDir, "eclipse: all");
        assertOnce(eclipseOutput, "Generate Eclipse files on " + baseDir.resolve("a"));
        assertOnce(eclipseOutput, "Generate Eclipse files on " + baseDir.resolve("b"));
        JkPathTree.of(baseDir).deleteRoot();
    }

    private String run(Path baseDir, String cmdLine) {
        JkProcResult result = executor.prepareJeka(false, baseDir, cmdLine)
                .setFailOnError(false)
                .setCollectStdout(true)
                .setCollectStderr(true)
                .exec();
        String output = result.getStdoutAsString() + result.getStderrAsString();
        Assertions.assertEquals(0, result.getExitCode(), output);
        return output;
    }

    private static void assertOnce(String output, String text) {
        int count = output.split(java.util.regex.Pattern.quote(text), -1).length - 1;
        Assertions.assertEquals(1, count, "'" + text + "' found " + count + " times in:\n" + output);
    }

}

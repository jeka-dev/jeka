package e2e;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkProcResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The shell scripts expand '::xxx' shorthands themselves when running a program with '-p'.
 */
class ShellShorthandTest {

    private final JekaCmdLineExecutor executor = new JekaCmdLineExecutor();

    @Test
    void shorthand_quotedProgramArgs_keptAsOneArg() throws Exception {
        Path baseDir = Files.createTempDirectory("jk-shell-shorthand");
        Files.createDirectories(baseDir.resolve("jeka-src/app"));
        Files.writeString(baseDir.resolve("jeka-src/app/Main.java"),
                "package app;\n"
                + "public class Main {\n"
                + "  public static void main(String[] args) {\n"
                + "    for (String arg : args) System.out.println(\"arg=[\" + arg + \"]\");\n"
                + "  }\n"
                + "}\n");
        Files.writeString(baseDir.resolve("jeka.properties"),
                "@base=on\n"
                + "@base.mainClass=app.Main\n"
                + "jeka.cmd.hi=-p \"Hello World\" 'it is' plain\n");

        JkProcResult result = executor.prepareJeka(false, baseDir, "::hi")
                .setFailOnError(false)
                .setCollectStdout(true)
                .setCollectStderr(true)
                .exec();

        String output = result.getStdoutAsString() + result.getStderrAsString();
        Assertions.assertEquals(0, result.getExitCode(), output);
        Assertions.assertTrue(output.contains("arg=[Hello World]"), output);
        Assertions.assertTrue(output.contains("arg=[it is]"), output);
        Assertions.assertTrue(output.contains("arg=[plain]"), output);
        Assertions.assertFalse(output.contains("\"Hello"), output);
        JkPathTree.of(baseDir).deleteRoot();
    }

}

package e2e;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkProcResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Arguments after '-p' belong to the program: the shell scripts must not read them as their own options.
 */
class ShellProgramArgsTest {

    private final JekaCmdLineExecutor executor = new JekaCmdLineExecutor();

    @Test
    void cleanFlagAfterProgram_passedToProgram_jekaOutputKept() throws Exception {
        Path baseDir = Files.createTempDirectory("jk-shell-program-args");
        Files.createDirectories(baseDir.resolve("jeka-src/app"));
        Files.writeString(baseDir.resolve("jeka-src/app/Main.java"),
                "package app;\n"
                + "public class Main {\n"
                + "  public static void main(String[] args) {\n"
                + "    for (String arg : args) System.out.println(\"arg=[\" + arg + \"]\");\n"
                + "  }\n"
                + "}\n");
        Files.writeString(baseDir.resolve("jeka.properties"), "@base=on\n@base.mainClass=app.Main\n");

        // Builds the program
        Assertions.assertEquals(0, run(baseDir, "-p first").getExitCode());
        Path marker = baseDir.resolve("jeka-output/marker.txt");
        Files.writeString(marker, "kept");

        JkProcResult result = run(baseDir, "-p -c -v");
        String output = result.getStdoutAsString() + result.getStderrAsString();
        Assertions.assertEquals(0, result.getExitCode(), output);
        Assertions.assertTrue(output.contains("arg=[-c]"), output);
        Assertions.assertTrue(output.contains("arg=[-v]"), output);
        Assertions.assertTrue(Files.exists(marker), "jeka-output was cleaned: " + output);
        JkPathTree.of(baseDir).deleteRoot();
    }

    private JkProcResult run(Path baseDir, String cmdLine) {
        return executor.prepareJeka(false, baseDir, cmdLine)
                .setFailOnError(false)
                .setCollectStdout(true)
                .setCollectStderr(true)
                .exec();
    }

}

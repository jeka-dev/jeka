package e2e;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkProcResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

class ChildBaseOptionTest {

    private final JekaCmdLineExecutor executor = new JekaCmdLineExecutor();

    @Test
    void childBase_unknownOrDeclared() throws Exception {
        Path baseDir = Files.createTempDirectory("jk-child-base");
        Files.writeString(baseDir.resolve("jeka.properties"), "_jeka.child-bases=mods/a\n");
        Files.createDirectories(baseDir.resolve("mods/a"));
        Files.writeString(baseDir.resolve("mods/a/jeka.properties"), "");

        // Unknown child base: fails, unless --force
        JkProcResult unknown = run(baseDir, "-cb=mods/b base: info");
        Assertions.assertNotEquals(0, unknown.getExitCode());
        String output = unknown.getStdoutAsString() + unknown.getStderrAsString();
        Assertions.assertTrue(output.contains("No child base found at location mods/b"), output);
        Assertions.assertEquals(0, run(baseDir, "-cb=mods/b base: info --force").getExitCode());

        // Declared child base, whatever the separator
        Assertions.assertEquals(0, run(baseDir, "-cb=mods/a base: info").getExitCode());
        Assertions.assertEquals(0, run(baseDir, "-cb=mods/a/ base: info").getExitCode());
        Assertions.assertEquals(0, run(baseDir, "-cb=mods\\a base: info").getExitCode());

        // KBean existing only in the child
        Files.createDirectories(baseDir.resolve("mods/a/jeka-src"));
        Files.writeString(baseDir.resolve("mods/a/jeka-src/ChildOnly.java"),
                "public class ChildOnly extends dev.jeka.core.tool.KBean {\n"
                + "  public void hi() { System.out.println(\"hi from \" + getBaseDir().getFileName()); }\n"
                + "}\n");
        JkProcResult childOnly = run(baseDir, "-cb=mods/a childOnly: hi");
        String childOnlyOutput = childOnly.getStdoutAsString() + childOnly.getStderrAsString();
        Assertions.assertEquals(0, childOnly.getExitCode(), childOnlyOutput);
        Assertions.assertTrue(childOnlyOutput.contains("hi from a"), childOnlyOutput);
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

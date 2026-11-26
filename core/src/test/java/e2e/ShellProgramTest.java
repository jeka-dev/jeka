package e2e;

import dev.jeka.core.api.file.JkPathTree;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

class ShellProgramTest {

    private final JekaCmdLineExecutor executor = new JekaCmdLineExecutor();

    @Test
    void program_withJavadocAndSourcesJars_runsMainJar() throws Exception {
        Path baseDir = Files.createTempDirectory("jk-shell-program");
        Files.writeString(baseDir.resolve("jeka.properties"), "");
        Path output = Files.createDirectories(baseDir.resolve("jeka-output"));

        // Any runnable jar does it: the one of JeKa prints its version
        Path coreJar = executor.getJekaShellPath().resolveSibling("dev.jeka.jeka-core.jar");
        Files.copy(coreJar, output.resolve("app.jar"));

        // Sort before 'app.jar', as when created by a publication
        Files.writeString(output.resolve("app-javadoc.jar"), "not a jar");
        Files.writeString(output.resolve("app-sources.jar"), "not a jar");

        // Fails on a non-zero exit code, as 'Invalid or corrupt jarfile'
        String stdout = executor.prepareJeka(false, baseDir, "-p --version").exec().getStdoutAsString();

        Assertions.assertFalse(stdout.isBlank());
        JkPathTree.of(baseDir).deleteRoot();
    }

}

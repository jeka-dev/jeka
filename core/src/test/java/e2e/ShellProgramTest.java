package e2e;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkProcResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

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

    @Test
    void program_withJarWithoutMainClass_failsWithMessage() throws Exception {
        Path baseDir = Files.createTempDirectory("jk-shell-program-lib");
        Files.writeString(baseDir.resolve("jeka.properties"), "");
        Path output = Files.createDirectories(baseDir.resolve("jeka-output"));
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (JarOutputStream os = new JarOutputStream(Files.newOutputStream(output.resolve("lib.jar")), manifest)) {
            os.putNextEntry(new JarEntry("a.txt"));
        }

        JkProcResult result = executor.prepareJeka(false, baseDir, "-p")
                .setFailOnError(false)
                .setCollectStderr(true)
                .exec();

        Assertions.assertNotEquals(0, result.getExitCode());
        Assertions.assertTrue(result.getStderrAsString().contains("has no Main-Class"), result.getStderrAsString());
        JkPathTree.of(baseDir).deleteRoot();
    }

}

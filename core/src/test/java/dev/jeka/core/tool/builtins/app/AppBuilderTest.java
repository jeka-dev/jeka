package dev.jeka.core.tool.builtins.app;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.utils.JkUtilsSystem;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

class AppBuilderTest {

    @Test
    void createShellFile_passesArgumentsAsIs() throws Exception {
        Assumptions.assumeFalse(JkUtilsSystem.IS_WINDOWS);
        Path dir = Files.createTempDirectory("jk-app-builder");
        Path workDir = Files.createDirectories(dir.resolve("work"));
        Files.createFile(workDir.resolve("file-to-not-expand"));

        // A fake 'jeka' printing its arguments, one per line
        Path binDir = Files.createDirectories(dir.resolve("bin"));
        Path fakeJeka = Files.writeString(binDir.resolve("jeka"),
                "#!/bin/sh\nfor arg in \"$@\"; do echo \"$arg\"; done\n");
        fakeJeka.toFile().setExecutable(true);
        Path shellFile = AppBuilder.createShellFile(dir.resolve("app"), dir.resolve("cowsay.jar"));

        ProcessBuilder processBuilder = new ProcessBuilder(shellFile.toString(), "*", "a b")
                .directory(workDir.toFile())
                .redirectErrorStream(true);
        processBuilder.environment().put("PATH", binDir + ":" + System.getenv("PATH"));
        Process process = processBuilder.start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        process.waitFor();

        List<String> args = Arrays.asList(output.split("\n"));
        Assertions.assertEquals(List.of("-r", dir.resolve("app").toString(), "-p", "*", "a b"), args);
        JkPathTree.of(dir).deleteRoot();
    }

}

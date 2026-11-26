package dev.jeka.plugins.nodejs;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.utils.JkUtilsSystem;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class JkNodeJsTest {

    @Test
    void exec_notNpmNorNpx_fails() {
        JkNodeJs nodeJs = JkNodeJs.ofVersion("22.12.0");
        Assertions.assertThrows(IllegalArgumentException.class, () -> nodeJs.exec("node --version"));
    }

    @Test
    void exec_npmAndNpx_runInWorkingDirWithArguments() throws IOException {
        Assumptions.assumeFalse(JkUtilsSystem.IS_WINDOWS);
        Path installDir = Files.createTempDirectory("jk-nodejs");
        Path workingDir = Files.createDirectories(installDir.resolve("work"));
        writeFakeCommand(installDir, "npm");
        writeFakeCommand(installDir, "npx");

        JkNodeJs.of(installDir)
                .setWorkingDir(workingDir)
                .exec("npm install 'a b'")
                .exec("npx cowsay hello");

        Assertions.assertEquals("install|a b|\n", Files.readString(workingDir.resolve("npm.txt")));
        Assertions.assertEquals("cowsay|hello|\n", Files.readString(workingDir.resolve("npx.txt")));
        JkPathTree.of(installDir).deleteRoot();
    }

    // Writes its arguments, separated by '|', in <name>.txt of the working dir
    private static void writeFakeCommand(Path installDir, String name) throws IOException {
        Path file = Files.createDirectories(installDir.resolve("bin")).resolve(name);
        Files.writeString(file, "#!/bin/sh\nfor arg in \"$@\"; do printf '%s|' \"$arg\"; done > " + name + ".txt\n"
                + "echo >> " + name + ".txt\n");
        file.toFile().setExecutable(true);
    }

}

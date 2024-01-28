package dev.jeka.core;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsPath;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * End-to-end tests about scaffolding.
 * Supposed to be run in dev.jeka.core working dir.
 */
class CoreScaffoldTester extends JekaCommandLineExecutor {

    void run() {

        // Basic scaffold and checks
        scaffoldAndCheckInTemp("self#scaffold -lv", "help", true);
        scaffoldAndCheckInTemp("project#scaffold", "help", true);

        // Check IntelliJ + Eclipse metadata
        Path workingDir = scaffoldAndCheckInTemp("project#scaffold", "project#clean project#pack", false);
        runWithDistribJekaShell(workingDir, "eclipse#files");
        runWithDistribJekaShell(workingDir, "intellij#iml");
        JkUtilsAssert.state(Files.exists(workingDir.resolve("src/main/java")),
                "No source tree has been created when scaffolding Java.");
        JkPathTree.of(workingDir).deleteRoot();
    }

    private Path scaffoldAndCheckInTemp(String scaffoldCmdLine, String checkCommandLine, boolean deleteAfter) {
        Path path = JkUtilsPath.createTempDirectory("jeka-scaffold-test-");
        runWithDistribJekaShell(path, scaffoldCmdLine);
        runWithDistribJekaShell(path, checkCommandLine);
        runWithBaseDirJekaShell(path, checkCommandLine);
        if (deleteAfter) {
            JkPathTree.of(path).deleteRoot();
        }
        return path;
    }

}

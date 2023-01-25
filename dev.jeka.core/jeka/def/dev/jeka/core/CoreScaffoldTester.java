package dev.jeka.core;

import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsPath;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * End-to-end tests about scaffolding.
 * Supposed to be run in dev.jeka.core working dir.
 */
class CoreScaffoldTester extends JekaCommandLineExecutor {

    CoreScaffoldTester(JkProperties properties) {
        super("..", properties);
    }

    void run() {
        scaffold("scaffold#run -lv", "help", false);
        Path projectDir = scaffold("scaffold#run scaffold#wrapper", "help", false);
        scaffold("scaffold#run scaffold#wrapper", "help", true);
        projectDir = scaffold("scaffold#run project#", "project#clean project#pack", false);
        runJeka(projectDir.toString(), "eclipse#files");
        runJeka(projectDir.toString(), "intellij#iml");
        JkUtilsAssert.state(Files.exists(projectDir.resolve("src/main/java")),
                "No source tree has been created when scaffolding Java.");
    }

    private Path scaffold(String scaffoldCmdLine, String checkCommandLine, boolean checkWithWrapper) {
        Path path = JkUtilsPath.createTempDirectory("jeka-scaffold-test-");
        runJeka(path.toString(), scaffoldCmdLine);
        runJeka(checkWithWrapper, path.toString(), checkCommandLine);
        return path;
    }

}

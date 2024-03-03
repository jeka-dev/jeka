package dev.jeka.core;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.tool.builtins.tooling.ide.IntellijKBean;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * End-to-end tests about scaffolding.
 * Supposed to be run in dev.jeka.core working dir.
 */
class ScaffoldTester extends JekaCommandLineExecutor {

    void run() {

        // Basic scaffold and checks
        scaffoldAndCheckInTemp("base: scaffold -v", "hello base: info -v -Djeka.java.version=17", true);
        scaffoldAndCheckInTemp("base: scaffold scaffold.kind=APP -vi", "base: test runMain", true);
        scaffoldAndCheckInTemp("project: scaffold scaffold.template=BUILD_CLASS", ": --help", true);

        // Scaffold template=PROPS + layout=SIMPLE
        Path tempDir = scaffoldAndCheckInTemp(
                "project: scaffold.template=PROPS layout.style=SIMPLE scaffold",
                "project: pack -v", false);
        JkUtilsAssert.state(Files.exists(tempDir.resolve(JkProject.DEPENDENCIES_TXT_FILE)),
                "dependencies.txt has not been generated");
        JkPathTree.of(tempDir).deleteRoot();

        // Scaffold for Jeka  plugin
        scaffoldAndCheckInTemp(
                "project: scaffold scaffold.template=PLUGIN",
                "project: pack", true);

        // Check IntelliJ + Eclipse metadata
        Path workingDir = scaffoldAndCheckInTemp(
                "project: scaffold",
                "project: clean pack", false);
        runWithDistribJekaShell(workingDir, "eclipse: files");
        runWithDistribJekaShell(workingDir, "intellij: iml -D" + IntellijKBean.IML_SKIP_MODULE_XML_PROP + "=true");
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

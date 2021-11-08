package dev.jeka.core;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsPath;

import java.nio.file.Path;

/**
 * End-to-end tests about scaffolding.
 * Supposed to be run in dev.jeka.core working dir.
 */
class CoreScaffoldTester extends JekaCommandLineExecutor {

    CoreScaffoldTester() {
        super("..");
    }

    void run() {
        scaffold("scaffold#run", "help", false);
        Path dir = scaffold("scaffold#run scaffold#wrap", "help", false);
        scaffold("scaffold#run scaffold#wrap -scaffold#wrapDelegatePath="
                + dir, "help", true);
        dir = scaffold("scaffold#run java#", "clean java#pack", false);
        runjeka(dir.toString(), "eclipse#files eclipse#all");
        runjeka(dir.toString(), "intellij#iml intellij#modulesXml");
    }

    private Path scaffold(String scaffoldCmdLine, String checkCommandLine, boolean checkWithWrapper) {
        Path path = JkUtilsPath.createTempDirectory("jeka-scaffold-test-");
        runjeka(path.toString(), scaffoldCmdLine);
        runjeka(checkWithWrapper, path.toString(), checkCommandLine);
        return path;
    }

    public static void main(String[] args) throws Exception {
        JkLog.setDecorator(JkLog.Style.INDENT);
        new CoreScaffoldTester().run();
    }

}

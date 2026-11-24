package dev.jeka.core.tool.builtins.tooling.ide;

import dev.jeka.core.api.java.JkJavaProcess;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.tool.Main;

import java.nio.file.Path;
import java.util.Collections;

class JekaExec {

    private JekaExec() {
    }

    // Runs in a distinct process, cause the engine relies on static state
    static void exec(Path baseDir, String... args) {
        JkJavaProcess.ofJava(Main.class.getName())
                .setClasspath(Collections.singletonList(JkLocator.getJekaJarPath().toAbsolutePath()))
                .setWorkingDir(baseDir.toAbsolutePath().normalize())
                .addParams(args)
                .setInheritIO(true)
                .exec();
    }

}

package dev.jeka.core;


import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.docker.JkDocker;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class DockerTester  {

    static void run() {
        if (!JkDocker.isPresent()) {
            JkLog.warn("Docker not present. Can't run Docker tests");
        }
        JkDocker.prepareExec("build")
                .addParamsAsCmdLine("--progress=plain --no-cache -t jeka-install-ubuntu .")
                .setWorkingDir(dockerDir())
                .exec();
    }

    static Path dockerDir() {
        String candidate = "dev.jeka.core/jeka-src/dev/jeka/core";
        if (Files.isDirectory(Paths.get(candidate))) {
            return Paths.get(candidate);
        }
        return Paths.get("../" + candidate);
    }

    public static void main(String[] args) {
        JkLog.setDecorator(JkLog.Style.FLAT);
        run();
    }

}

package dev.jeka.core;


import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.docker.JkDocker;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

class DockerTester  {

    private static final String IMAGE_NAME = "jeka-install-ubuntu";

    private static final Path DOCKER_DIR = dockerDir();

    private static final boolean NO_CACHE = false;

    static void run() {
        if (!JkDocker.isPresent()) {
            JkLog.warn("Docker not present. Can't run Docker tests");
            return;
        }
        buildImage();
        //runImage();
    }

    static void buildImage() {

        JkDocker.prepareExec("build")
                .addParamsIf(NO_CACHE, "--no-cache")
                .addParamsAsCmdLine("--build-arg CACHEBUST=%s", Instant.now())
                .addParamsAsCmdLine("--progress=plain -t %s .", IMAGE_NAME)
                .setWorkingDir(DOCKER_DIR)
                .exec();
    }

    static void runImage() {
        JkDocker.prepareExec("run", "-rm", "-t", IMAGE_NAME)
                .setWorkingDir(DOCKER_DIR)
                .exec();
    }

    private static Path dockerDir() {
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

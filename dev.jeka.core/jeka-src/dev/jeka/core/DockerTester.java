/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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
        buildInstallImage();
        //runImage();
    }

    static void buildInstallImage() {

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

    private static void testBuildAndRunImage() {

    }

    public static void main(String[] args) {
        JkLog.setDecorator(JkLog.Style.INDENT);
        run();
    }

}

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

import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.docker.JkDocker;
import dev.jeka.core.api.tooling.docker.JkDockerBuild;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DockerImageMaker {

    public static final String GROUP = "jekadev";

    public static final String IMAGE_NAME = GROUP + "/jeka";

    // Run : docker run -v $HOME/.jeka/cache4c:/cache -v .:/workdir jeka --version
    public static void createImage() {
        JkDockerBuild dockerBuild = JkDockerBuild.of();
        Path jekaDist = Paths.get("jeka-output/distrib/bin");
        if (!Files.exists(jekaDist)) {
            jekaDist = Paths.get("dev.jeka.core").resolve(jekaDist);
        }
        if(!Files.exists(jekaDist)) {
            jekaDist = Paths.get("..").resolve(jekaDist).normalize();
        }
        dockerBuild.setUserId(null); // Use only root user
        dockerBuild.rootSteps
                .add("RUN apt update")
                .add("RUN apt install -y curl && apt install -y unzip && apt install -y git");
        dockerBuild.nonRootSteps
                .add("RUN mkdir /workdir && mkdir root/.jeka && mkdir /cache && ln -s /cache root/.jeka/cache")
                .addCopy(jekaDist.resolve("jeka"), "/root/.jeka/bin/")
                .addCopy(jekaDist.resolve("dev.jeka.jeka-core.jar"), "/root/.jeka/bin/")
                .add("RUN chmod +x /root/.jeka/bin/jeka")

                .add("WORKDIR /workdir")
                .add("ENTRYPOINT [\"/root/.jeka/bin/jeka\"]");
        Path ctxDir = dockerBuild.buildImage(IMAGE_NAME);
        System.out.println("Jeka image built from " + ctxDir);
    }

    public static void pushImage(String version, String password) {
        String imageNameWithVersion = IMAGE_NAME + ":" + version;
        JkDocker.exec("image", "tag", IMAGE_NAME, imageNameWithVersion);
        JkDocker.exec("login", "-u", GROUP, "-p", password);
        JkDocker.exec("push", imageNameWithVersion);
        if (!JkVersion.of(version).isSnapshot()) {
            String imageNameWithLatest= IMAGE_NAME + ":latest";
            JkDocker.exec("image", "tag", IMAGE_NAME, imageNameWithLatest);
            JkDocker.exec("push", imageNameWithLatest);
        }
        JkDocker.exec("logout");
    }

    public static void main(String[] args) {
        JkLog.setDecorator(JkLog.Style.FLAT);
        JkLog.setVerbosity(JkLog.Verbosity.VERBOSE);
        createImage();
    }
}

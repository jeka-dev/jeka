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

package dev.jeka.core.api.tooling.docker;

import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class JkDockerBuildTest {

    @Test
    public void simple_root() throws Exception {
        if (!JkDocker.isPresent()) {
            return;
        }
        final Path extraFile = Paths.get(JkDockerBuildTest.class.getResource("toto.txt").toURI());
        JkDockerBuild dockerBuild = JkDockerBuild.of();
        dockerBuild.addExtraFile(extraFile, "/my_toto.txt");
        dockerBuild.addFooterStatement("ENTRYPOINT [\"echo\", \"toto\"]");
        dockerBuild.setUserId(null);
        String imageName = "jk-test-simpleroot";
        System.out.println(dockerBuild.dockerBuildContent(imageName));
        System.out.println(dockerBuild.info(imageName));
        dockerBuild.buildImage(imageName);
        JkDocker.exec("run", imageName);
        JkDocker.prepareExec("rmi", "IMAGE", imageName);
    }

    @Test
    public void simple_nonroot() {
        if (!JkDocker.isPresent()) {
            return;
        }
        simpleNonRootWithBaseImage("ubuntu");
        simpleNonRootWithBaseImage("alpine:latest");
        simpleNonRootWithBaseImage(JkDockerJvmBuild.BASE_IMAGE);
    }

    private void simpleNonRootWithBaseImage(String baseImage) {
        JkDockerBuild dockerBuild = JkDockerBuild.of();
        dockerBuild.addFooterStatement("ENTRYPOINT [\"echo\", \"toto\"]");
        dockerBuild.setBaseImage(baseImage);
        //dockerBuild.setUserId(1001);
        //dockerBuild.setGroupId("1002");
        String imageName = "jk-test-simplenonroot";
        System.out.println(dockerBuild.dockerBuildContent(baseImage));
        System.out.println(dockerBuild.info(imageName));
        dockerBuild.buildImage(imageName + "-" + baseImage);
        JkDocker.exec("run", imageName);
        JkDocker.prepareExec("rmi", "IMAGE", imageName);
    }

}
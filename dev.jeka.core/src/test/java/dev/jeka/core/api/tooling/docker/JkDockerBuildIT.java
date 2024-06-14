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

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsPath;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class JkDockerBuildIT {

    @Test
    public void simple_root() throws Exception {
        if (!JkDocker.isPresent()) {
            return;
        }
        JkLog.setDecorator(JkLog.Style.INDENT);
        //JkLog.setVerbosity(JkLog.Verbosity.VERBOSE);
        final Path extraFile = Paths.get(JkDockerBuildIT.class.getResource("toto.txt").toURI());
        JkDockerBuild dockerBuild = JkDockerBuild.of();
        dockerBuild.nonRootSteps.addCopy(extraFile, "/my_toto.txt", false);
        dockerBuild.nonRootSteps.add("ENTRYPOINT [\"echo\"]");
        dockerBuild.nonRootSteps.add("CMD [\"titi\"]");
        dockerBuild.setUserId(null);
        String imageName = "jk-test-simpleroot";
        System.out.println(dockerBuild.render());
        Path contextDir = JkUtilsPath.createTempDirectory("jk-docker-ctx");
        System.out.println("Cereating buidcontext in " + contextDir);
        dockerBuild.buildImage(contextDir, imageName);
        JkDocker.prepareExec("run", imageName)
                .setInheritIO(false)
                .addParams("lulu")
                .setLogWithJekaDecorator(true).
                exec();
        JkDocker.prepareExec("image", "rm", "--force", imageName)
                .setInheritIO(false).setLogWithJekaDecorator(true).exec();
        System.out.println("Generated in " + contextDir);
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
        dockerBuild.nonRootSteps.add("ENTRYPOINT [\"echo\", \"toto\"]");
        dockerBuild.setBaseImage(baseImage);
        //dockerBuild.setUserId(1001);
        //dockerBuild.setGroupId("1002");
        String imageName = "jk-test-simplenonroot-" + baseImage;
        System.out.println(dockerBuild.render());
        Path contextDir = JkUtilsPath.createTempDirectory("jk-docker-ctx");
        dockerBuild.buildImage(contextDir, imageName);
        JkDocker.prepareExec("run", imageName)
                .setInheritIO(false).setLogWithJekaDecorator(true).exec();
        JkDocker.prepareExec("image", "rm", "--force", imageName)
                .setInheritIO(false).setLogWithJekaDecorator(true).exec();
    }

}
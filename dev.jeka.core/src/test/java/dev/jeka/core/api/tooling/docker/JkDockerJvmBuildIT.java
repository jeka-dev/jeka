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

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkTransitivity;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsPath;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Path;

public class JkDockerJvmBuildIT {

    @Test
    @Ignore  // Fails in automated test cause no jeka-output/classes dir exist
    public void simple() {
        if (!JkDocker.isPresent()) {
            return;
        }
        JkLog.setDecorator(JkLog.Style.INDENT);
        JkProject project = JkProject.of().flatFacade()
                .customizeCompileDeps(deps -> deps
                        .and("com.google.guava:guava:23.0", JkTransitivity.NONE)
                        .and("javax.servlet:javax.servlet-api:4.0.1"))
                .customizeRuntimeDeps(deps -> deps
                        .and("org.postgresql:postgresql:42.2.19")
                        .withTransitivity("com.google.guava:guava", JkTransitivity.RUNTIME)
                        .minus("javax.servlet:javax.servlet-api"))
                .customizeTestDeps(deps -> deps
                        .and(JkDependencySet.Hint.first(), "org.mockito:mockito-core:2.10.0")
                )
                .setModuleId("my:project").setVersion("MyVersion")
                .setMainClass("dev.jeka.core.tool.Main")
                .getProject();
        JkDockerJvmBuild dockerJvmBuild = JkDockerJvmBuild.of(project);
        dockerJvmBuild.nonRootSteps.insertBefore("COPY classpath.txt", "# rem for testing 'insertBefore'");
        String imageName = "jk-testunit-jvm";

        System.out.println(dockerJvmBuild.info(imageName));

        // now run the image
        Path contextDir =JkUtilsPath.createTempDirectory("jk-test");
        System.out.println(contextDir);
        dockerJvmBuild.buildImage(contextDir, imageName);
        JkDocker.prepareExec("run", imageName)
                .setLogCommand(true)
                .addParams("--version")
                .setInheritIO(false)
                .setLogWithJekaDecorator(true)
                .exec();
        JkDocker.prepareExec("image", "rm", "--force", imageName);

    }

}
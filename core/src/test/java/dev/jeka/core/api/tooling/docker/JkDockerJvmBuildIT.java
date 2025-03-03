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
        if (!JkDocker.of().isPresent()) {
            return;
        }
        JkLog.setDecorator(JkLog.Style.INDENT);

        JkDockerJvmBuild dockerJvmBuild = JkDockerJvmBuild.of(project().asBuildable());
        dockerJvmBuild.dockerfileTemplate
                .moveCursorBefore("COPY classpath.txt")
                .add( "# rem for testing 'insertBefore'");
        String imageName = "jk-testunit-jvm";

        System.out.println(dockerJvmBuild.renderInfo());

        // now run the image
        Path contextDir =JkUtilsPath.createTempDirectory("jk-test");
        System.out.println(contextDir);
        dockerJvmBuild.buildImage(contextDir, imageName);
        JkDocker.of().addParams("run", imageName)
                .setLogCommand(true)
                .addParams("--version")
                .setInheritIO(false)
                .setLogWithJekaDecorator(true)
                .exec();
        JkDocker.of().addParams("image", "rm", "--force", imageName);
    }

    private JkProject project() {
        JkProject project = JkProject.of();
        project.compilation.dependencies
                .add("com.google.guava:guava:23.0", JkTransitivity.NONE)
                .add("javax.servlet:javax.servlet-api:4.0.1");
        project.packaging.runtimeDependencies
                .add("org.postgresql:postgresql:42.2.19")
                .remove("javax.servlet:javax.servlet-api");
        project.testing.compilation.dependencies
                .add("org.mockito:mockito-core:2.10.0");
        project.flatFacade
                .setModuleId("my:project").setVersion("MyVersion")
                .setMainClass("dev.jeka.core.tool.Main");
        return project;
    }

}
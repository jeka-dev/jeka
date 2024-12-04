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
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsPath;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

public class JkDockerJvmBuildTest {

    @Test
    public void testAgents() {
        JkLog.setDecorator(JkLog.Style.INDENT);

        JkProject project = project();
        if (!Files.exists(project.getBaseDir().resolve("jeka-output/classes"))) {
            return;  // Fails in automated test cause no jeka-output/classes dir exist
        }
        JkDockerJvmBuild dockerJvmBuild = JkDockerJvmBuild.of(project.asBuildable());
        dockerJvmBuild.addAgent("io.opentelemetry.javaagent:opentelemetry-javaagent:2.10.0", "myAgentOption");
        System.out.println(dockerJvmBuild.renderInfo());

        // Test docker files is correct
        Assert.assertTrue(dockerJvmBuild.renderDockerfile().contains("/app/agents/"));

        // Test build dir is properly generated
        Path dir = JkUtilsPath.createTempDirectory("jk-test");
        dockerJvmBuild.generateContextDir(dir);
        Assert.assertTrue("No files found in " + dir.resolve("agents"),
                JkPathTree.of(dir.resolve("agents")).containFiles());
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
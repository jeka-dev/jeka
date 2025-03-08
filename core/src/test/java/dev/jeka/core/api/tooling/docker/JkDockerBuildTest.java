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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class JkDockerBuildTest {

    @Test
    void testMoveCursor() {
        JkDockerBuild dockerBuild = JkDockerBuild.of();

        // Add Root
        dockerBuild.dockerfileTemplate.moveCursorBeforeUserNonRoot().moveCursorNext()
            .add("## 1")
            .add("## 2")
            .add("## 3")
            .add("## 4");
        String result = getResult(dockerBuild);

        // Remove Last
        dockerBuild.dockerfileTemplate
                .moveCursorBefore("## 4")
                .remove();
        result = getResult(dockerBuild);
        Assertions.assertFalse(result.contains("## 4"));

        // Add it back
        dockerBuild.dockerfileTemplate.add("## 4bis");
        result = getResult(dockerBuild);
        Assertions.assertTrue(result.endsWith("## 4bis"));

        // Insert a step
        dockerBuild.dockerfileTemplate.moveCursorBefore("## 3")
            .add("## 2.5")
            .add("## 2.6");
        result = getResult(dockerBuild);
        Assertions.assertTrue(result.contains("## 2\n## 2.5\n## 2.6\n## 3"));
    }

    @Test
    public void testAddNonRootUser() {

        // In default mode the nonroot user is created on ubuntu
        JkDockerBuild dockerBuild = JkDockerBuild.of();
        dockerBuild.setBaseImage("ubuntu:latest");
        String result = getResult(dockerBuild);
        Assertions.assertTrue(result.contains("USER nonroot"));

        // In default mode the nonroot user is not created on distroless non-root
        dockerBuild.setBaseImage("gcr.io/distroless/static-debian12:nonroot");
        result = getResult(dockerBuild);
        Assertions.assertFalse(result.contains("USER nonroot"));
    }

    private String getResult(JkDockerBuild dockerBuild) {
        String result = dockerBuild.renderDockerfile();
        System.out.println(dockerBuild.dockerfileTemplate.render(dockerBuild));
        System.out.println("----------------------------- baseImage=" + dockerBuild.getBaseImage());
        System.out.println(result);
        System.out.println("-----------------------------");
        System.out.println("-----------------------------");
        System.out.println();
        return result;
    }

}
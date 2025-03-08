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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

class JkDockerBuildIT {

    @Test
    public void simple() throws Exception {
        if (!JkDocker.of().isPresent()) {
            return;
        }
        JkLog.setDecorator(JkLog.Style.INDENT);
        //JkLog.setVerbosity(JkLog.Verbosity.VERBOSE);
        final Path extraFile = Paths.get(JkDockerBuildIT.class.getResource("toto.txt").toURI());

        JkDockerBuild dockerBuild = JkDockerBuild.of();

        // Add Root
        dockerBuild.dockerfileTemplate.moveCursorBeforeUserNonRoot();
        dockerBuild.dockerfileTemplate.add("## Comment appended in root user section");
        dockerBuild.dockerfileTemplate.add("## 2nd Comment appended in root user section");

        // Add non root
        dockerBuild.dockerfileTemplate.moveCursorBeforeUserNonRoot().moveCursorNext();
        dockerBuild.dockerfileTemplate.addCopy(extraFile, "/my_toto.txt", false);
        dockerBuild.dockerfileTemplate.add("ENTRYPOINT [\"echo\"]");
        dockerBuild.dockerfileTemplate.add("CMD [\"titi\"]");
        String imageName = "jk-test-simpleroot";

        // render
        System.out.println("----------- raw docker file");
        System.out.println(dockerBuild.dockerfileTemplate.render(dockerBuild));
        System.out.println("----------- resolved docker file");
        System.out.println(dockerBuild.renderDockerfile());

        Path contextDir = JkUtilsPath.createTempDirectory("jk-docker-ctx");
        System.out.println("Cereating buidcontext in " + contextDir);
        dockerBuild.buildImage(contextDir, imageName);
        JkDocker.of().addParams("run", imageName)
                .setInheritIO(false)
                .addParams("lulu")
                .setLogWithJekaDecorator(true).
                exec();
        JkDocker.of().addParams("image", "rm", "--force", imageName)
                .setInheritIO(false).setLogWithJekaDecorator(true).exec();
        System.out.println("Generated in " + contextDir);
    }

    @Test
    @Disabled
    void javaFromScratch() throws URISyntaxException {
        if (!JkDocker.of().isPresent()) {
            return;
        }
        final Path certFile = Paths.get(JkDockerBuildIT.class.getResource("my-cert.jks").toURI());
        final Path jarFile = Paths.get(JkDockerBuildIT.class.getResource("hello-jeka.jar").toURI());
        JkLog.setDecorator(JkLog.Style.INDENT);

        JkDockerBuild dockerBuild = JkDockerBuild.of()
            .setBaseImage("eclipse-temurin:21-jdk-alpine")
            .setExposedPorts(8080);
        dockerBuild.dockerfileTemplate
                .addCopy(jarFile, "/app/my-app.jar")
                .add("WORKDIR /app")
                .addEntrypoint("java", "-jar", "/app/my-app.jar")
                .moveCursorBeforeUserNonRoot()
                .addCopy(certFile, "/app/my-cert.jks")
                .add("RUN keytool -import -file /app/my-cert.jks -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit");
        System.out.println(dockerBuild.renderDockerfile());
        Path contextDir = JkUtilsPath.createTempDirectory("jk-docker-ctx");
        dockerBuild.buildImage(contextDir, "my-demo-image");
        System.out.println(contextDir);
    }

    @Test
    void simple_nonroot() {
        if (!JkDocker.of().isPresent()) {
            return;
        }
        JkLog.setDecorator(JkLog.Style.INDENT);
        JkLog.setVerbosity(JkLog.Verbosity.VERBOSE);
        simpleNonRootWithBaseImage("alpine:latest");
        simpleNonRootWithBaseImage(JkDockerJvmBuild.DEFAULT_BASE_IMAGE);
        simpleNonRootWithBaseImage("ubuntu");
    }

    private void simpleNonRootWithBaseImage(String baseImage) {
        JkDockerBuild dockerBuild = JkDockerBuild.of();
        dockerBuild.dockerfileTemplate.moveCursorBeforeUserNonRoot().moveCursorNext();
        dockerBuild.dockerfileTemplate.addEntrypoint("echo", "toto");
        dockerBuild.setBaseImage(baseImage);
        String imageName = "jk-test-simplenonroot-" + baseImage;
        System.out.println("----------- raw docker file");
        System.out.println(dockerBuild.dockerfileTemplate.render(dockerBuild));
        System.out.println("----------- raw docker file");
        System.out.println(dockerBuild.renderDockerfile());
        Path contextDir = JkUtilsPath.createTempDirectory("jk-docker-ctx");
        dockerBuild.buildImage(contextDir, imageName);
        JkDocker.of().addParams("run", imageName)
                .setInheritIO(false).setLogWithJekaDecorator(true).exec();
        JkDocker.of().addParams("image", "rm", "--force", imageName)
                .setInheritIO(false).setLogWithJekaDecorator(true).exec();
    }

}
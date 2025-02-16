/*
 * Copyright 2014-2025  the original author or authors.
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
import dev.jeka.core.api.testing.JkApplicationTester;
import dev.jeka.core.api.utils.JkUtilsNet;
import dev.jeka.core.api.utils.JkUtilsString;

import java.nio.file.Path;
import java.util.function.Consumer;

public class JkDockerAppTester extends JkApplicationTester {

    private Integer port;

    private String baseUrl = "http://localhost";

    private final JkDockerBuild dockerBuild;

    private final Consumer<String> tester;

    private String baseUrlAndPort;

    private boolean showAppLogs = false;

    private String imageName;

    private Path contextPath;

    private String containerName;

    private JkDockerAppTester(JkDockerBuild dockerBuild, Consumer<String> tester) {
        this.dockerBuild = dockerBuild;
        this.tester = tester;
        startTimeout = 30*1000;
    }

    public static JkDockerAppTester of(JkDockerBuild dockerBuild, Consumer<String> tester) {
        return new JkDockerAppTester(dockerBuild, tester);
    }

    @Override
    protected void startApp() {
        startTimeout = 30*1000;
        port = port == null ? findFreePort() : port;
        baseUrlAndPort = baseUrl + ":" + port;
        String effectiveImageName =  JkUtilsString.isBlank(imageName) ? "jeka-docker-tester-" + port
                : imageName;
        containerName = effectiveImageName + "-container";
        if (contextPath == null) {
            dockerBuild.buildImageInTemp(effectiveImageName);
        } else {
            dockerBuild.buildImage(contextPath, effectiveImageName);
        }

        JkDocker.of().addParams("run", "-d", "-p", String.format("%s:8080", port), "--name",
                        containerName, effectiveImageName)
                .setInheritIO(showAppLogs)
                .setLogWithJekaDecorator(false)
                .exec();
    }

    @Override
    protected boolean isApplicationReady() {
        return JkUtilsNet.isAvailableAndOk(baseUrl, JkLog.isDebug());
    }

    @Override
    protected void executeTests() {
        tester.accept(baseUrlAndPort);
    }

    @Override
    protected void stopGracefully() {
        JkDocker.of().addParams("rm", "-f", containerName)
                .setInheritIO(false).setLogWithJekaDecorator(true)
                .exec();
    }

    /**
     * Sets the port for the application to use. If left empty, a random port will be choosen.
     */
    public JkDockerAppTester setPort(int port) {
        this.port = port;
        return this;
    }

    /**
     * Sets the base URL for the application. Default is <i>http://localhost</i>.
     */
    public JkDockerAppTester setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    /**
     * Configures whether application logs should be displayed during the testing process.
     */
    public JkDockerAppTester setShowAppLogs(boolean showAppLogs) {
        this.showAppLogs = showAppLogs;
        return this;
    }

    public JkDockerAppTester setContextPath(Path contextPath) {
        this.contextPath = contextPath;
        return this;
    }

    public JkDockerAppTester setImageName(String imageName) {
        this.imageName = imageName;
        return this;
    }

    public <T extends JkDockerBuild> T getDockerBuild() {
        return (T) dockerBuild;
    }
}

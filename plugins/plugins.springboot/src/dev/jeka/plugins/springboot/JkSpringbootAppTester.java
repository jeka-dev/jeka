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

package dev.jeka.plugins.springboot;

import dev.jeka.core.api.project.JkBuildable;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.testing.JkApplicationTester;
import dev.jeka.core.api.utils.JkUtilsNet;
import dev.jeka.core.api.utils.JkUtilsString;

import java.util.function.Consumer;

/**
 * A specialized implementation of JkApplicationTester for testing Spring Boot applications.
 * It provides mechanisms to start the application, verify its readiness, run tests, and stop it
 * gracefully, leveraging the Spring Boot actuator endpoints and customizable options.
 */
public class JkSpringbootAppTester extends JkApplicationTester {

    private Integer port;

    private String baseUrl = "http://localhost";

    private String extraJavaOptions = "";

    private final JkBuildable buildable;

    private final Consumer<String> tester;

    private String baseUrlAndPort;

    private boolean showAppLogs = false;

    private JkSpringbootAppTester(JkBuildable.Supplier buildable, Consumer<String> tester) {
        this.buildable = buildable.asBuildable();
        this.tester = tester;
        startTimeout = 30*1000;
    }

    /**
     * Creates a new {@code JkSpringbootAppTester} instance initialized with the specified buildable
     * and tester.
     *
     * @param buildable the project or base under testing
     * @param tester    the consumer responsible for handling the output from the application being tested.
     *                  It consumes the base url of the application to test.
     */
    public static JkSpringbootAppTester of(JkBuildable.Supplier buildable, Consumer<String> tester) {
        return new JkSpringbootAppTester(buildable, tester);
    }

    @Override
    protected void init() {
        port = port == null ? findFreePort() : port;
        baseUrlAndPort = baseUrl + ":" + port; super.init();
    }

    @Override
    public void startApp() {
        buildable.prepareRunJar()
                .addJavaOptions("-Dserver.port=" + port)
                .addJavaOptions("-Dmanagement.endpoint.shutdown.enabled=true")
                .addJavaOptions("-Dmanagement.endpoints.web.exposure.include=*")
                .addJavaOptions(JkUtilsString.parseCommandline(extraJavaOptions))
                .setLogCommand(JkLog.isVerbose())
                .setInheritIO(showAppLogs)
                .setLogWithJekaDecorator(false)
                .exec();
    }

    @Override
    public boolean isApplicationReady() {
        return JkUtilsNet.isStatusOk(baseUrlAndPort + "/actuator/health", JkLog.isDebug());
    }

    @Override
    public void executeTests() {
        tester.accept(baseUrlAndPort);
    }

    @Override
    public void stopGracefully() {
        String shutdownUrl = baseUrlAndPort + "/actuator/shutdown";
        JkLog.verbose("Invoke %s", shutdownUrl);
        JkUtilsNet.sendHttpRequest(shutdownUrl, "POST", null).asserOk();
    }

    /**
     * Sets the port for the application to use. If left empty, a random port will be choosen.
     */
    public JkSpringbootAppTester setPort(int port) {
        this.port = port;
        return this;
    }

    /**
     * Sets the base URL for the application. Default is <i>http://localhost</i>.
     */
    public JkSpringbootAppTester setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    /**
     * Sets additional Java options for the application testing process.
     * These options are passed as arguments to the Java process and can be used
     * to configure the runtime environment.
     */
    public JkSpringbootAppTester setExtraJavaOptions(String extraJavaOptions) {
        this.extraJavaOptions = extraJavaOptions;
        return this;
    }

    /**
     * Configures whether application logs should be displayed during the testing process.
     */
    public JkSpringbootAppTester setShowAppLogs(boolean showAppLogs) {
        this.showAppLogs = showAppLogs;
        return this;
    }

}

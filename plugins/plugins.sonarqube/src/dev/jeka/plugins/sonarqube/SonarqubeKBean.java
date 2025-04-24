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

package dev.jeka.plugins.sonarqube;

import dev.jeka.core.api.depmanagement.JkDepSuggest;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkDocUrl;
import dev.jeka.core.tool.JkPostInit;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;

import java.nio.file.Files;

@JkDoc("Runs Sonarqube analysis and checks quality gates. \n" +
        "The properties prefixed with 'sonar.', such as '-Dsonar.host.url=http://myserver/..', " +
        "will be appended to the SonarQube configuration.")
@JkDocUrl("https://github.com/jeka-dev/jeka/tree/master/plugins/dev.jeka.plugins.sonarqube")
public class SonarqubeKBean extends KBean {

    @JkDoc("If true, the list of production dependency files will be provided to sonarqube")
    public boolean provideProductionLibs = true;

    @JkDoc("If true, the list of test dependency files will be provided to sonarqube")
    public boolean provideTestLibs = false;

    @JkDoc("Version of the SonarQube client to run. It can be '+' for the latest one (at the price of a greater process time). " +
            "The version will be resolved against 'org.sonarsource.scanner.cli:sonar-scanner-cli' coordinate. " +
            "Use a blank string to use the client embedded in the plugin.")
    @JkDepSuggest(versionOnly = true , hint = "org.sonarsource.scanner.cli:sonar-scanner-cli:")
    public String scannerVersion = JkSonarqube.DEFAULT_SCANNER__VERSION;

    @JkDoc("If true, displays sonarqube output on console")
    public boolean logOutput = true;

    @JkDoc("Ping the sonarqube server prior running analysis")
    public boolean pingServer = true;

    @JkDoc("If true, the quality gate will be registered alongside analysis in project quality checkers.")
    public boolean gate;

    private JkSonarqube sonarqube;

    @Override
    protected void init() {
        sonarqube = JkSonarqube.ofVersion(getRunbase().getDependencyResolver().getRepos(),
                JkSonarqube.DEFAULT_SCANNER__VERSION);
        sonarqube.setVersion(getRunbase().getDependencyResolver().getRepos(), effectiveScannerVersion());
        sonarqube.setPingServer(pingServer);
        sonarqube.setLogOutput(logOutput);
        sonarqube.setProperties(getRunbase().getProperties());
    }

    @JkDoc("Runs a SonarQube analysis and sends the results to a Sonar server.")
    public void run() {
        JkProject project = load(ProjectKBean.class).project;
        JkUtilsAssert.state(Files.exists(project.compilation.layout.resolveClassDir()),
                "Project class directory not found. " +
                "Please run compilation and tests prior running analysis.");
        sonarqube.configureFor(project, provideProductionLibs, provideTestLibs);
        sonarqube.run();
    }

    @JkDoc("Checks if the analysed project passes its quality gates. " +
            "The 'run' method is expected to have already been executed.")
    public void check() {
        JkSonarqube.QualityGateResponse response = sonarqube.checkQualityGate();
        if (response.success) {
            JkLog.info("Sonarqube quality gate passed successfully.");
        } else {
            JkLog.error("Project does not meet quality gate criteria. See %s/dashboard?id=%s",
                    sonarqube.getHostUrl(), sonarqube.getProperty(JkSonarqube.PROJECT_KEY) );
            System.exit(1);
        }
    }

    @JkDoc("Adds Sonarqube analysis to quality checkers")
    @JkPostInit(required = true)
    private void postInit(ProjectKBean projectKBean) {
        projectKBean.project.qualityCheck.add("sonarqube", () -> {
            run();
            if (gate) {
                check();
            }
        });
    }

    public JkSonarqube getSonarqube() {
        return sonarqube;
    }

    private String effectiveScannerVersion() {
        return JkUtilsString.isBlank(scannerVersion) ? JkSonarqube.DEFAULT_SCANNER__VERSION : scannerVersion;
    }

}

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
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkException;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;

import java.nio.file.Files;
import java.nio.file.Path;

@JkDoc("Run SonarQube analysis.")
public class SonarqubeKBean extends KBean {

    private JkProject project;

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

    public final JkSonarqube sonarqube = JkSonarqube.ofVersion(getRunbase().getDependencyResolver().getRepos(),
            JkSonarqube.DEFAULT_SCANNER__VERSION);

    @JkDoc("Runs sonarQube analysis based on properties defined in this plugin. " +
            "Properties prefixed with 'sonar.' as '-sonar.host.url=http://myserver/..' " +
            "will be appended to sonarQube properties.")
    public void run() {
        JkUtilsAssert.state(project != null, "Np project to analyse found in %s", getBaseDir());
        JkUtilsAssert.state(Files.exists(project.compilation.layout.resolveClassDir()),
                "Project class directory not found. " +
                "Please run compilation and tests prior running analysis.");
        sonarqube.configureFor(project, provideProductionLibs, provideTestLibs);
        sonarqube.run();
    }

    @JkDoc("Checks if the analysed project passes its quality gates. " +
            "The 'run' method is expected to have already been executed.")
    public void check() {
        JkUtilsAssert.state(project != null, "Np project to analyse found in %s", getBaseDir());
        JkSonarqube.QualityGateResponse response = sonarqube.checkQualityGate();
        if (response.success) {
            JkLog.info("Sonarqube quality gate passed successfully.");
        } else {
            JkLog.error("Project does not meet quality gate criteria. See %s/dashboard?id=%s",
                    sonarqube.getHostUrl(), sonarqube.getProperty(JkSonarqube.PROJECT_KEY) );
            System.exit(1);
        }
    }

    @Override
    protected void init() {
        sonarqube.setVersion(getRunbase().getDependencyResolver().getRepos(), effectiveScannerVersion());
        sonarqube.setPingServer(pingServer);
        sonarqube.setLogOutput(logOutput);
        sonarqube.setProperties(getRunbase().getProperties());
        getRunbase().find(ProjectKBean.class).ifPresent(projectKBean -> {
            project = projectKBean.project;
        });
    }

    private String effectiveScannerVersion() {
        return JkUtilsString.isBlank(scannerVersion) ? JkSonarqube.DEFAULT_SCANNER__VERSION : scannerVersion;
    }

}

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
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;

@JkDoc("Run SonarQube analysis.")
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

    public final JkSonarqube sonarqube = JkSonarqube.ofVersion(getRunbase().getDependencyResolver().getRepos(),
            JkSonarqube.DEFAULT_SCANNER__VERSION);

    @JkDoc("Runs sonarQube analysis based on properties defined in this plugin. " +
            "Properties prefixed with 'sonar.' as '-sonar.host.url=http://myserver/..' " +
            "will be appended to sonarQube properties.")
    public void run() {
        getRunbase().find(ProjectKBean.class).ifPresent(projectKBean -> {
            sonarqube.configureFor(projectKBean.project, provideProductionLibs, provideTestLibs);
        });
        sonarqube.run();
    }

    @Override
    protected void init() {
        sonarqube.setVersion(getRunbase().getDependencyResolver().getRepos(), effectiveScannerVersion());
        sonarqube.setPingServer(pingServer);
        sonarqube.setLogOutput(logOutput);
        sonarqube.setProperties(getRunbase().getProperties());
    }

    private String effectiveScannerVersion() {
        return JkUtilsString.isBlank(scannerVersion) ? JkSonarqube.DEFAULT_SCANNER__VERSION : scannerVersion;
    }

}

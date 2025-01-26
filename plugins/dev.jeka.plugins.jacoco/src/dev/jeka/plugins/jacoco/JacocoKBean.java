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

package dev.jeka.plugins.jacoco;

import dev.jeka.core.api.depmanagement.JkDepSuggest;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;

@JkDoc("Run unit tests with Jacoco agent coverage test tool.")
public class JacocoKBean extends KBean {

    @JkDoc("If true, project from ProjectJkBean will be configured with Jacoco automatically.")
    public boolean configureProject = true;

    @JkDoc("If true, Jacoco will produce a standard XML report usable by Sonarqube.")
    public boolean xmlReport = true;

    @JkDoc("If true, Jacoco will produce a standard HTML report .")
    public boolean htmlReport = true;

    @JkDoc("Options string, as '[option1]=[value1],[option2]=[value2]', to pass to agent as described here : https://www.jacoco.org/jacoco/trunk/doc/agent.html")
    public String agentOptions;

    @JkDoc("Exclusion patterns separated with ',' to exclude some class files from the XML report input. An example is 'META-INF/**/*.jar'.")
    public String classDirExcludes;

    @JkDoc("Version of Jacoco to use both for agent and report. The version will be resolved against coordinate " +
            "'org.jacoco:org.jacoco.agent'")
    @JkDepSuggest(versionOnly = true, hint = "org.jacoco:org.jacoco.agent")
    public String jacocoVersion = JkJacoco.DEFAULT_VERSION;

    @Override
    protected void init() {
        find(ProjectKBean.class).ifPresent(projectKBean ->
                configureForDefaultProject(projectKBean.project));
    }

    private void configureForDefaultProject(JkProject project) {
        if (!configureProject) {
            return;
        }
        JkJacoco jacoco = JkJacoco.ofVersion(project.dependencyResolver, jacocoVersion)
                .setHtmlReport(htmlReport)
                .setXmlReport(xmlReport);
        if (!JkUtilsString.isBlank(classDirExcludes)) {
            jacoco.setClassDirFilter(classDirExcludes);
        }
        if (!JkUtilsString.isBlank(this.agentOptions)) {
            jacoco.addAgentOptions(agentOptions.split(","));
        }
        jacoco.configureAndApplyTo(project);
    }

}

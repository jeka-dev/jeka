package dev.jeka.plugins.jacoco;

import dev.jeka.core.api.depmanagement.JkDepSuggest;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.builtins.project.ProjectJkBean;

@JkDoc("Run unit tests with Jacoco agent coverage test tool.")
public class JacocoJkBean extends JkBean {

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
    public String jacocoVersion = "0.8.7";


    private JacocoJkBean() {
        getRuntime().load(ProjectJkBean.class).lazily(this::configureForDefaultProject);
    }


    private void configureForDefaultProject(JkProject project) {
        if (!configureProject) {
            return;
        }
        configure(project);
    }

    private void configure(JkProject project) {
        JkJacoco jacoco = JkJacoco.ofVersion(project.dependencyResolver, jacocoVersion)
                .setHtmlReport(htmlReport)
                .setXmlReport(xmlReport);
        if (!JkUtilsString.isBlank(classDirExcludes)) {
            jacoco.setClassDirFilter(classDirExcludes);
        }
        if (!JkUtilsString.isBlank(this.agentOptions)) {
            jacoco.addAgentOptions(agentOptions.split(","));
        }
        jacoco.configureForAndApplyTo(project);
    }





}

package dev.jeka.plugins.jacoco;

import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkClass;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkDocPluginDeps;
import dev.jeka.core.tool.JkPlugin;
import dev.jeka.core.tool.builtins.java.JkPluginJava;
import dev.jeka.core.tool.builtins.repos.JkPluginRepo;

@JkDoc("Run unit tests with Jacoco agent coverage test tool.")
@JkDocPluginDeps(JkPluginJava.class)
public class JkPluginJacoco extends JkPlugin {

    /**
     * Relative location to the output folder of the generated jacoco report file
     */
    public static final String OUTPUT_RELATIVE_PATH = "jacoco/jacoco.exec";

    public static final String OUTPUT_XML_RELATIVE_PATH = "jacoco/jacoco.xml";

    protected JkPluginJacoco(JkClass run) {
        super(run);
    }

    @JkDoc("If false, tests will be run without Jacoco.")
    public boolean enabled = true;

    @JkDoc("If true, Jacoco will produce a standard XML report usable by Sonarqube.")
    public boolean xmlReport = true;

    @JkDoc("Options string, as '[option1]=[value1],[option2]=[value2]', to pass to agent as described here : https://www.jacoco.org/jacoco/trunk/doc/agent.html")
    public String agentOptions;

    @JkDoc("Exclusion patterns separated with ',' to exclude some class files from the XML report input. An example is 'META-INF/**/*.jar'.")
    public String classDirExcludes;

    @JkDoc("Version of Jacoco to use both for agent and report.")
    public String jacocoVersion = "0.8.7";

    @Override
    protected void afterSetup() {
        if (!enabled) {
            return;
        }
        JkPluginJava pluginJava = getJkClass().getPlugins().get(JkPluginJava.class);
        final JkJavaProject project = pluginJava.getProject();
        final JkJacoco jacoco;
        if (JkUtilsString.isBlank(jacocoVersion)) {
            jacoco = JkJacoco.ofEmbedded();
        } else {
            jacoco = JkJacoco.ofManaged(project.getConstruction().getDependencyResolver(), jacocoVersion);
        }
        jacoco.setExecFile(project.getOutputDir().resolve(OUTPUT_RELATIVE_PATH))
            .setClassDir(project.getConstruction().getCompilation().getLayout().getClassDirPath());
        if (xmlReport) {
            jacoco.addReportOptions("--xml",
                    project.getOutputDir().resolve(OUTPUT_XML_RELATIVE_PATH).toString());
        }
        if (!JkUtilsString.isBlank(classDirExcludes)) {
            JkPathMatcher pathMatcher = JkPathMatcher.of(false, classDirExcludes.split(","));
            jacoco.setClassDirFilter(pathMatcher);
        }
        if (!JkUtilsString.isBlank(this.agentOptions)) {
            jacoco.addAgentOptions(agentOptions.split(","));
        }
        jacoco.configure(project.getConstruction().getTesting().getTestProcessor());
    }
    
}

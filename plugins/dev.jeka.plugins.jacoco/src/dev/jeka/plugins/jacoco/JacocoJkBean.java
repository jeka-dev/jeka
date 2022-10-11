package dev.jeka.plugins.jacoco;

import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.builtins.project.ProjectJkBean;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@JkDoc("Run unit tests with Jacoco agent coverage test tool.")
public class JacocoJkBean extends JkBean {

    /**
     * Relative location to the output folder of the generated jacoco report file
     */
    public static final String OUTPUT_RELATIVE_PATH = "jacoco/jacoco.exec";

    public static final String OUTPUT_XML_RELATIVE_PATH = "jacoco/jacoco.xml";

    public static final String OUTPUT_HTML_RELATIVE_PATH = "jacoco/html";  // this is a folder

    @JkDoc("If false, project from ProjectJkBean won't be configured for Jacoco.")
    public boolean enabled = true;

    @JkDoc("If true, Jacoco will produce a standard XML report usable by Sonarqube.")
    public boolean xmlReport = true;

    @JkDoc("If true, Jacoco will produce a standard HTML report .")
    public boolean htmlReport = false;

    @JkDoc("Options string, as '[option1]=[value1],[option2]=[value2]', to pass to agent as described here : https://www.jacoco.org/jacoco/trunk/doc/agent.html")
    public String agentOptions;

    @JkDoc("Exclusion patterns separated with ',' to exclude some class files from the XML report input. An example is 'META-INF/**/*.jar'.")
    public String classDirExcludes;

    @JkDoc("Version of Jacoco to use both for agent and report.")
    public String jacocoVersion = "0.8.7";

    private ProjectJkBean projectPlugin = getRuntime().getBean(ProjectJkBean.class).configure(this::configureForDefaultProject);

    private JkJacoco jacoco;

    /**
     * Generates XML and HTML export reports from the exec file
     */
    public JacocoJkBean generateExport() {
        jacoco.generateExport();
        return this;
    }

    private void configureForDefaultProject(JkProject project) {
        if (!enabled) {
            return;
        }
        configure(project);
    }

    public void configure(JkProject project) {
        jacoco = JkJacoco.ofManaged(project.getDependencyResolver(), jacocoVersion);
        jacoco.setExecFile(project.getOutputDir().resolve(OUTPUT_RELATIVE_PATH))
            .setClassDir(project.getCompilation().getLayout().getClassDirPath());
        if (xmlReport) {
            jacoco.addReportOptions("--xml",
                    project.getOutputDir().resolve(OUTPUT_XML_RELATIVE_PATH).toString());
        }
        if (htmlReport) {
            jacoco.addReportOptions("--html",
                    project.getOutputDir().resolve(OUTPUT_HTML_RELATIVE_PATH).toString());
        }
        if (!JkUtilsString.isBlank(classDirExcludes)) {
            JkPathMatcher pathMatcher = JkPathMatcher.of(false, classDirExcludes.split(","));
            jacoco.setClassDirFilter(pathMatcher);
        }
        if (!JkUtilsString.isBlank(this.agentOptions)) {
            jacoco.addAgentOptions(agentOptions.split(","));
        }
        jacoco.configure(project.getTesting().getTestProcessor());
        List<Path> sourceDirs =project.getCompilation()
                .getLayout().getSources().getRootDirsOrZipFiles().stream()
                .map(path -> path.isAbsolute() ? path : project.getBaseDir().resolve(path))
                .collect(Collectors.toList());
        jacoco.setSources(sourceDirs);
    }

    public JacocoJkBean setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public JacocoJkBean setXmlReport(boolean xmlReport) {
        this.xmlReport = xmlReport;
        return this;
    }

    public JacocoJkBean setHtmlReport(boolean htmlReport) {
        this.htmlReport = htmlReport;
        return this;
    }

    public JacocoJkBean setClassDirExcludes(String classDirExcludes) {
        this.classDirExcludes = classDirExcludes;
        return this;
    }

    public JacocoJkBean setJacocoVersion(String jacocoVersion) {
        this.jacocoVersion = jacocoVersion;
        return this;
    }

    public AgentJarAndReportFile getAgentAndReportFile() {
        JkUtilsAssert.state(jacoco != null,
                "This method cannot be invoked when no project has been configured on.");
        return new AgentJarAndReportFile(jacoco.getToolProvider().getAgentJar(), jacoco.getExecFile());
    }

    public static class AgentJarAndReportFile {

        private final Path agentPath;

        private final Path reportFile;

        AgentJarAndReportFile(Path agentPath, Path reportFile) {
            this.agentPath = agentPath;
            this.reportFile = reportFile;
        }

        public Path getAgentPath() {
            return agentPath;
        }

        public Path getReportFile() {
            return reportFile;
        }
    }

}

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

    public final JkSonarqube sonarqube = JkSonarqube.ofEmbedded();

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
        if (JkUtilsString.isBlank(scannerVersion)) {
            sonarqube.setVersion(null, null);
        } else {
            sonarqube.setVersion(getRunbase().getDependencyResolver().getRepos(), scannerVersion);
        }
        sonarqube.setPingServer(pingServer);
        sonarqube.setLogOutput(logOutput);
        sonarqube.setProperties(getRunbase().getProperties());
    }

}

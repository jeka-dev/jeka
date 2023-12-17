package dev.jeka.plugins.sonarqube;

import dev.jeka.core.api.depmanagement.JkDepSuggest;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.builtins.project.ProjectJkBean;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@JkDoc("Run SonarQube analysis.")
public class SonarqubeJkBean extends JkBean {

    @JkDoc("If false, no sonar analysis will be performed")
    public boolean enabled = true;

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

    private Consumer<JkSonarqube> sonarqubeConfigurer = sonarqube -> {};

    private Supplier<List<JkProject>> projectsSupplier = null;

    @JkDoc("Runs sonarQube analysis based on properties defined in this plugin. " +
            "Properties prefixed with 'sonar.' as '-sonar.host.url=http://myserver/..' " +
            "will be appended to sonarQube properties.")
    public void run() {
        if (!enabled) {
            JkLog.info("Sonarqube analysis has been disabled. No analysis will be performed.");
            return;
        }
        List<JkProject> projects = projectsSupplier == null
                ? Collections.singletonList(getRuntime().load(ProjectJkBean.class).getProject())
                : projectsSupplier.get();
        for (JkProject project : projects) {
            JkSonarqube sonarqube = createConfiguredSonarqube(project);
            sonarqubeConfigurer.accept(sonarqube);
            sonarqube.run();
        }
    }

    /**
     * By default, this KBean configures Sonarqube to scan the project defined in the {@link ProjectJkBean}.
     * You can specify explicitly the projects to scan by using this method.
     */
    public SonarqubeJkBean configureProjectsToScan(Supplier<JkProject> ...projectSuppliers) {
        this.projectsSupplier = () -> {
            List<JkProject> projects = new LinkedList<>();
            for (Supplier<JkProject> supplier : projectSuppliers) {
                projects.add(supplier.get());
            }
            return projects;
        };
        return this;
    }

    /**
     * Adds a configurator for sonarqube that will be executed just before sonarqube analysis is run.
     * This ensures that configurator will be executed after all properties are set.
     */
    public SonarqubeJkBean lazily(Consumer<JkSonarqube> sonarqubeConfigurer) {
        this.sonarqubeConfigurer = sonarqubeConfigurer;
        return this;
    }

    /**
     * Use {@link #lazily(Consumer)}
     */
    @Deprecated
    public SonarqubeJkBean lately(Consumer<JkSonarqube> sonarqubeConfigurator) {
        return this.lazily(sonarqubeConfigurator);
    }

    private JkSonarqube createConfiguredSonarqube(JkProject project) {
        final JkSonarqube sonarqube;
        if (JkUtilsString.isBlank(scannerVersion)) {
            sonarqube = JkSonarqube.ofEmbedded();
        } else {
            sonarqube = JkSonarqube.ofVersion(project.dependencyResolver.getRepos(),
                    scannerVersion);
        }
        return sonarqube
                .configureFor(project, provideProductionLibs, provideTestLibs)
                .setLogOutput(logOutput)
                .setProperties(getRuntime().getProperties());
    }


}

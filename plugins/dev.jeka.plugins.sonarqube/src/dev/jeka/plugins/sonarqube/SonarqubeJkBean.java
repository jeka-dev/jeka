package dev.jeka.plugins.sonarqube;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkModuleId;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.project.JkCompileLayout;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.project.JkProjectConstruction;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.*;
import dev.jeka.core.tool.builtins.project.ProjectJkBean;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

@JkDoc("Run SonarQube analysis.")
public class SonarqubeJkBean extends JkBean {

    private final Map<String, String> properties = new HashMap<>();

    @JkDoc("If false, no sonar analysis will be performed")
    public boolean enabled = true;

    @JkDoc("If true, the list of production dependency files will be provided to sonarqube")
    public boolean provideProductionLibs = true;

    @JkDoc("If true, the list of test dependency files will be provided to sonarqube")
    public boolean provideTestLibs = false;

    @JkDoc("Version of the SonarQube client to run. It can be '+' for the latest one, at the price of a greater process time.\n" +
            "Use a blank string to use the client embedded in the plugin.")
    public String scannerVersion = "4.6.2.2472";

    @JkDoc("If true, displays sonarqube output on console")
    public boolean logOutput = true;

    private Consumer<JkSonarqube> sonarqubeConfigurer = sonarqube -> {};

    private Supplier<List<JkProject>> projectsSupplier = null;

    /**
     * Creates a {@link JkSonarqube} object configured for the supplied {@link JkProject}.
     */
    public JkSonarqube createConfiguredSonarqube(JkProject project) {
        final JkCompileLayout prodLayout = project.getConstruction().getCompilation().getLayout();
        final JkCompileLayout testLayout = project.getConstruction().getTesting().getCompilation().getLayout();
        final Path baseDir = project.getBaseDir();
        JkPathSequence libs = JkPathSequence.of();
        JkProjectConstruction construction = project.getConstruction();
        if (provideProductionLibs) {
            JkDependencySet deps = construction.getCompilation().getDependencies()
                    .merge(construction.getRuntimeDependencies()).getResult();
            libs = project.getConstruction().getDependencyResolver().resolve(deps).getFiles();
        }
        final Path testReportDir = project.getConstruction().getTesting().getReportDir();
        JkModuleId moduleId = project.getPublication().getGroupAndName();
        if (moduleId == null) {
            String baseDirName = baseDir.getFileName().toString();
            if (JkUtilsString.isBlank(baseDirName)) {
                baseDirName = baseDir.toAbsolutePath().getFileName().toString();
            }
            moduleId = JkModuleId.of(baseDirName, baseDirName);
        }
        final String version = project.getPublication().getVersion().getValue();
        final String fullName = moduleId.getDotedName();
        final String name = moduleId.getName();
        final JkSonarqube sonarqube;
        if (JkUtilsString.isBlank(scannerVersion)) {
            sonarqube = JkSonarqube.ofEmbedded();
        } else {
            sonarqube = JkSonarqube.ofVersion(project.getConstruction().getDependencyResolver().getRepos(),
                    scannerVersion);
        }
        sonarqube
                .setLogOutput(logOutput)
                .setProjectId(fullName, name, version)
                .setProperties(JkProperties.getAllStartingWith("sonar."))
                .setProjectBaseDir(baseDir)
                .setBinaries(project.getConstruction().getCompilation().getLayout().resolveClassDir())
                .setProperty(JkSonarqube.SOURCES, prodLayout.resolveSources().getRootDirsOrZipFiles())
                .setProperty(JkSonarqube.TEST, testLayout.resolveSources().getRootDirsOrZipFiles())
                .setProperty(JkSonarqube.WORKING_DIRECTORY, baseDir.resolve(JkConstants.JEKA_DIR + "/.sonar").toString())
                .setProperty(JkSonarqube.JUNIT_REPORTS_PATH,
                        baseDir.relativize( testReportDir.resolve("junit")).toString())
                .setProperty(JkSonarqube.SUREFIRE_REPORTS_PATH,
                        baseDir.relativize(testReportDir.resolve("junit")).toString())
                .setProperty(JkSonarqube.SOURCE_ENCODING, project.getConstruction().getSourceEncoding())
                .setProperty(JkSonarqube.JACOCO_XML_REPORTS_PATHS,
                    baseDir.relativize(project.getOutputDir().resolve("jacoco/jacoco.xml")).toString())
                .setProperty(JkSonarqube.JAVA_LIBRARIES, libs)
                .setProperty(JkSonarqube.JAVA_TEST_BINARIES, testLayout.getClassDirPath());
        if (provideTestLibs) {
            JkDependencySet deps = construction.getTesting().getCompilation().getDependencies();
            JkPathSequence testLibs = project.getConstruction().getDependencyResolver().resolve(deps).getFiles();
            sonarqube.setProperty(JkSonarqube.JAVA_TEST_LIBRARIES, testLibs);
        }
        return sonarqube;
    }

    @JkDoc("Runs sonarQube analysis based on properties defined in this plugin. " +
            "Properties prefixed with 'sonar.' as '-sonar.host.url=http://myserver/..' " +
            "will be appended to sonarQube properties.")
    public void run() {
        if (!enabled) {
            JkLog.info("Sonarqube analysis has been disabled. No analysis will be performed.");
            return;
        }
        List<JkProject> projects = projectsSupplier == null
                ? Collections.singletonList(getRuntime().getBean(ProjectJkBean.class).getProject())
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

    public SonarqubeJkBean configure(Consumer<JkSonarqube> sonarqubeConfigurer) {
        this.sonarqubeConfigurer = sonarqubeConfigurer;
        return this;
    }


}

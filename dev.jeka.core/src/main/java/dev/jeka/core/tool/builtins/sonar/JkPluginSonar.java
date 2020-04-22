package dev.jeka.core.tool.builtins.sonar;

import dev.jeka.core.api.depmanagement.JkModuleId;
import dev.jeka.core.api.depmanagement.JkScope;
import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.java.project.JkCompileLayout;
import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.tool.*;
import dev.jeka.core.tool.builtins.java.JkPluginJava;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@JkDoc("Run SonarQube analysis.")
@JkDocPluginDeps(JkPluginJava.class)
public class JkPluginSonar extends JkPlugin {

    private final Map<String, String> properties = new HashMap<>();

    public JkPluginSonar(JkCommandSet run) {
        super(run);
    }

    public static JkSonar configureSonarFrom(JkJavaProject project) {
        final JkCompileLayout prodLayout = project.getProduction().getCompilation().getLayout();
        final JkCompileLayout testLayout = project.getTesting().getCompilation().getLayout();
        final Path baseDir = project.getBaseDir();
        final JkPathSequence libs = project.getDependencyManagement().fetchDependencies(
                JkScope.RUNTIME, JkScope.PROVIDED).getFiles();
        final Path testReportDir = project.getTesting().getReportDir();
        final JkModuleId moduleId = project.getPublication().getModuleId();
        final JkVersion version = project.getPublication().getVersion();
        final String fullName = moduleId.getDotedName();
        final String name = moduleId.getName();
        return JkSonar
                .of(fullName, name, version)
                .withProperties(JkOptions.getAllStartingWith("sonar.")).withProjectBaseDir(baseDir)
                .withBinaries(project.getProduction().getCompilation().getLayout().resolveClassDir())
                .withLibraries(libs)
                .withSourcesPath(prodLayout.resolveSources().getRootDirsOrZipFiles())
                .withTestPath(testLayout.resolveSources().getRootDirsOrZipFiles())
                .withProperty(JkSonar.WORKING_DIRECTORY, baseDir.resolve(JkConstants.JEKA_DIR + "/.sonar").toString())
                .withProperty(JkSonar.JUNIT_REPORTS_PATH,
                        baseDir.relativize( testReportDir.resolve("junit")).toString())
                .withProperty(JkSonar.SUREFIRE_REPORTS_PATH,
                        baseDir.relativize(testReportDir.resolve("junit")).toString())
                .withProperty(JkSonar.SOURCE_ENCODING, project.getProduction().getCompilation().getSourceEncoding())
                .withProperty(JkSonar.JACOCO_REPORTS_PATHS,
                        baseDir.relativize(project.getOutputDir().resolve("jacoco/jacoco.exec")).toString());

    }

    @JkDoc("Runs a SonarQube analysis based on properties defined in this plugin. " +
            "Options prefixed with 'sonar.' as '-sonar.host.url=http://myserver/..' " +
            "will be appended to sonarQube properties.")
    public void run() {
        configureSonarFrom(getCommandSet().getPlugins().get(JkPluginJava.class).getProject()).withProperties(properties).run();
    }

    /**
     * Adds a property to setupAfterPluginActivations sonar instance to run. You'll find predefined keys in {@link JkSonar}.
     */
    public JkPluginSonar setProp(String key, String value) {
        this.properties.put(key, value);
        return this;
    }

}

package dev.jeka.core.tool.builtins.sonar;

import dev.jeka.core.api.depmanagement.JkJavaDepScopes;
import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.depmanagement.JkVersionedModule;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.api.java.project.JkProjectSourceLayout;
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
        final JkProjectSourceLayout sourceLayout = project.getSourceLayout();
        final Path baseDir = sourceLayout.getBaseDir();
        final JkPathSequence libs = project.getMaker().getDependencyResolver().resolve(project.getDependencies(),
                JkJavaDepScopes.RUNTIME, JkJavaDepScopes.PROVIDED).getFiles();
        final Path testReportDir = project.getMaker().getOutLayout().getTestReportDir();
        final JkVersionedModule module = project.getMaker().getSteps().getPublishing().getVersionedModule();
        final String fullName = module != null ? module.getModuleId().getDotedName() : project.getBaseDir().getFileName().toString();
        final String name = module != null ? module.getModuleId().getName() : project.getBaseDir().getFileName().toString();
        final JkVersion version = module != null ? module.getVersion() : JkVersion.of("");
        return JkSonar
                .of(fullName, name, version)
                .withProperties(JkOptions.getAllStartingWith("sonar.")).withProjectBaseDir(baseDir)
                .withBinaries(project.getMaker().getOutLayout().getClassDir())
                .withLibraries(libs)
                .withSourcesPath(sourceLayout.getSources().getRootDirsOrZipFiles())
                .withTestPath(sourceLayout.getTests().getRootDirsOrZipFiles())
                .withProperty(JkSonar.WORKING_DIRECTORY, sourceLayout.getBaseDir().resolve(JkConstants.JEKA_DIR + "/.sonar").toString())
                .withProperty(JkSonar.JUNIT_REPORTS_PATH,
                        baseDir.relativize( testReportDir.resolve("junit")).toString())
                .withProperty(JkSonar.SUREFIRE_REPORTS_PATH,
                        baseDir.relativize(testReportDir.resolve("junit")).toString())
                .withProperty(JkSonar.SOURCE_ENCODING, project.getMaker().getSteps().getCompilation().getSourceEncoding())
                .withProperty(JkSonar.JACOCO_REPORTS_PATHS,
                        baseDir.relativize(project.getMaker().getOutLayout().getOutputPath("jacoco/jacoco.exec")).toString());

    }

    @JkDoc("Runs a SonarQube analysis based on properties defined in this plugin. " +
            "Options prefixed with 'sonar.' as '-sonar.host.url=http://myserver/..' " +
            "will be appended to these properties.")
    public void run() {
        configureSonarFrom(getCommandSet().getPlugins().get(JkPluginJava.class).getProject()).withProperties(properties).run();
    }

    /**
     * Adds a property to setupAfterPluginActivations sonar instance to run. You'll find predefined keys in {@link JkSonar}.
     */
    public JkPluginSonar prop(String key, String value) {
        this.properties.put(key, value);
        return this;
    }

}

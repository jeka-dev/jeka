package org.jerkar.tool.builtins.sonar;

import org.jerkar.api.depmanagement.JkJavaDepScopes;
import org.jerkar.api.depmanagement.JkVersion;
import org.jerkar.api.depmanagement.JkVersionedModule;
import org.jerkar.api.file.JkPathSequence;
import org.jerkar.api.java.project.JkProjectSourceLayout;
import org.jerkar.api.java.project.JkJavaProject;
import org.jerkar.tool.*;
import org.jerkar.tool.builtins.java.JkPluginJava;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@JkDoc("Run SonarQube analysis.")
@JkDocPluginDeps(JkPluginJava.class)
public class JkPluginSonar extends JkPlugin {

    private final Map<String, String> properties = new HashMap<>();

    public JkPluginSonar(JkRun run) {
        super(run);
    }

    public static JkSonar configureSonarFrom(JkJavaProject project) {
        final JkProjectSourceLayout sourceLayout = project.getSourceLayout();
        final Path baseDir = sourceLayout.baseDir();
        final JkPathSequence libs = project.getMaker().getDependencyResolver().fetch(project.getDependencies(),
                JkJavaDepScopes.RUNTIME, JkJavaDepScopes.PROVIDED);
        final Path testReportDir = project.getMaker().getOutLayout().testReportDir();
        final JkVersionedModule module = project.getVersionedModule();
        final String fullName = module != null ? module.getModuleId().getDotedName() : project.getBaseDir().getFileName().toString();
        final String name = module != null ? module.getModuleId().getName() : project.getBaseDir().getFileName().toString();
        final JkVersion version = module != null ? module.getVersion() : JkVersion.of("");
        return JkSonar
                .of(fullName, name, version)
                .withProperties(JkOptions.getAllStartingWith("sonar.")).withProjectBaseDir(baseDir)
                .withBinaries(project.getMaker().getOutLayout().classDir())
                .withLibraries(libs)
                .withSourcesPath(sourceLayout.sources().getRootDirsOrZipFiles())
                .withTestPath(sourceLayout.tests().getRootDirsOrZipFiles())
                .withProperty(JkSonar.WORKING_DIRECTORY, sourceLayout.baseDir().resolve(JkConstants.JERKAR_DIR + "/.sonar").toString())
                .withProperty(JkSonar.JUNIT_REPORTS_PATH,
                        baseDir.relativize( testReportDir.resolve("junit")).toString())
                .withProperty(JkSonar.SUREFIRE_REPORTS_PATH,
                        baseDir.relativize(testReportDir.resolve("junit")).toString())
                .withProperty(JkSonar.SOURCE_ENCODING, project.getCompileSpec().getEncoding())
                .withProperty(JkSonar.JACOCO_REPORTS_PATHS,
                        baseDir.relativize(project.getMaker().getOutLayout().outputPath("jacoco/jacoco.exec")).toString());

    }

    @JkDoc("Runs a SonarQube analysis based on properties defined in this plugin. " +
            "Options prefixed with 'sonar.' as '-sonar.host.url=http://myserver/..' " +
            "will be appended to these properties.")
    public void run() {
        configureSonarFrom(getOwner().plugins().get(JkPluginJava.class).project()).withProperties(properties).run();
    }

    /**
     * Adds a property to postPluginSetup sonar instance to run. You'll find predefined keys in {@link JkSonar}.
     */
    public JkPluginSonar prop(String key, String value) {
        this.properties.put(key, value);
        return this;
    }

}

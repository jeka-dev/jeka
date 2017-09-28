package org.jerkar.plugins.sonar;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.jerkar.api.depmanagement.JkJavaDepScopes;
import org.jerkar.api.depmanagement.JkVersion;
import org.jerkar.api.depmanagement.JkVersionedModule;
import org.jerkar.api.file.JkPathSequence;
import org.jerkar.api.project.JkProjectSourceLayout;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.tool.JkBuild;
import org.jerkar.tool.JkBuildPlugin;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.JkJavaProjectBuild;
import org.jerkar.tool.JkOptions;

@JkDoc({"Add SonarQube capability to a build.",
        "The ananlysis is performed when the 'run' method is invoked.",
        "To parameterize this plugin just set the relevant sonar properies as options.",
        "For example you can launch the build whith '-sonar.host.url=http://myserver/..' to specify the SonarQube server url."})
public class JkBuildPluginSonar implements JkBuildPlugin {

    private final Map<String, String> properties = new HashMap<>();

    public static JkSonar configureSonarFrom(JkJavaProject project) {
        final JkProjectSourceLayout sourceLayout = project.getSourceLayout();
        final File baseDir = sourceLayout.baseDir();
        final JkPathSequence libs = project.maker().getDependencyResolver().get(project.getDependencies(),
                JkJavaDepScopes.RUNTIME, JkJavaDepScopes.PROVIDED);
        final File testReportDir = project.getOutLayout().testReportDir();
        final JkVersionedModule module = project.getVersionedModule();
        final String fullName = module != null ? module.moduleId().fullName() : project.getArtifactName();
        final String name = module != null ? module.moduleId().name() : project.getArtifactName();
        final JkVersion version = module != null ? module.version() : JkVersion.name("");
        return JkSonar
                .of(fullName, name, version)
                .withProperties(JkOptions.getAllStartingWith("sonar.")).withProjectBaseDir(baseDir)
                .withBinaries(project.getOutLayout().classDir())
                .withLibraries(libs)
                .withSources(sourceLayout.sources().roots())
                .withTest(sourceLayout.tests().roots())
                .withProperty(JkSonar.WORKING_DIRECTORY, sourceLayout.baseTree().file("build/.sonar").getPath())
                .withProperty(JkSonar.JUNIT_REPORTS_PATH,
                        JkUtilsFile.getRelativePath(baseDir, new File(testReportDir, "junit")))
                .withProperty(JkSonar.SUREFIRE_REPORTS_PATH,
                        JkUtilsFile.getRelativePath(baseDir, new File(testReportDir, "junit")))
                .withProperty(JkSonar.JACOCO_REPORTS_PATHS,
                        JkUtilsFile
                                .getRelativePath(baseDir, project.getOutLayout().outputFile("jacoco/jacoco.exec")));
    }

    @JkDoc("Launch a Sonar analysis")
    public void run(JkBuild build) {
        if (build instanceof JkJavaProjectBuild) {
            configureSonarFrom(((JkJavaProjectBuild) build).project()).withProperties(properties).run();
        } else {
            JkLog.warn("Plugin " + this.getClass().getSimpleName() + " not appliable for build type " + build.getClass());
        }
    }

    public JkBuildPluginSonar prop(String key, String value) {
        this.properties.put(key, value);
        return this;
    }

    @Override
    public void apply(JkBuild build) {
        // do nothing
    }

}

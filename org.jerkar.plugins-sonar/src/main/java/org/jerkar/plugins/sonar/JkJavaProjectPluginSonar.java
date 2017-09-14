package org.jerkar.plugins.sonar;

import org.jerkar.api.depmanagement.JkVersion;
import org.jerkar.api.depmanagement.JkVersionedModule;
import org.jerkar.api.file.JkPath;
import org.jerkar.api.project.JkProjectSourceLayout;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.api.project.java.JkJavaProjectPlugin;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.JkOptions;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@JkDoc({"Add SonarQube capability to a build.",
        "The ananlysis is performed when the 'verify' method is invoked.",
        "To parameterize this plugin just set the relevant sonar properies as options.",
        "For example you can launch the build whith '-sonar.host.url=http://myserver/..' to specify the SonarQube server url."})
public class JkJavaProjectPluginSonar implements JkJavaProjectPlugin {

    private final Map<String, String> properties = new HashMap<String, String>();
    private JkSonar jkSonar;

    public static JkSonar configureSonarFrom(JkJavaProject project) {
        JkProjectSourceLayout sourceLayout = project.getSourceLayout();
        final File baseDir = sourceLayout.baseDir();
        final JkPath libs = project.maker().getDependencyResolver().get(project.getDependencies(),
                JkJavaBuild.COMPILE, JkJavaBuild.PROVIDED);
        final File testReportDir = project.getOutLayout().testReportDir();
        JkVersionedModule module = project.getVersionedModule();
        String fullName = module != null ? module.moduleId().fullName() : project.getArtifactName();
        String name = module != null ? module.moduleId().name() : project.getArtifactName();
        JkVersion version = module != null ? module.version() : JkVersion.name("");
        return JkSonar
                .of(fullName, name, version)
                .withProperties(JkOptions.getAllStartingWith("sonar.")).withProjectBaseDir(baseDir)
                .withBinaries(project.getOutLayout().classDir()).withLibraries(libs)
                .withSources(sourceLayout.sources().roots())
                .withTest(sourceLayout.tests().roots())
                .withProperty(JkSonar.WORKING_DIRECTORY, sourceLayout.root().file("build/.sonar").getPath())
                .withProperty(JkSonar.JUNIT_REPORTS_PATH,
                        JkUtilsFile.getRelativePath(baseDir, new File(testReportDir, "junit")))
                .withProperty(JkSonar.SUREFIRE_REPORTS_PATH,
                        JkUtilsFile.getRelativePath(baseDir, new File(testReportDir, "junit")))
                .withProperty(JkSonar.JACOCO_REPORTS_PATH,
                        JkUtilsFile
                                .getRelativePath(baseDir, new File(testReportDir, "jacoco/jacoco.exec")));
    }

    @JkDoc("Launch a Sonar analysis.")
    public void verify() {
        jkSonar.run();
    }

    public JkJavaProjectPluginSonar prop(String key, String value) {
        this.properties.put(key, value);
        return this;
    }

    @Override
    public void accept(JkJavaProject project) {
       jkSonar = configureSonarFrom(project);
    }
}

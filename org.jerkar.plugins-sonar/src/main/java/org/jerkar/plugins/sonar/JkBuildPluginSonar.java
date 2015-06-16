package org.jerkar.plugins.sonar;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.jerkar.api.file.JkPath;
import org.jerkar.api.java.junit.JkUnit.JunitReportDetail;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.tool.JkBuild;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.JkOptions;
import org.jerkar.tool.builtins.templates.javabuild.JkJavaBuild;
import org.jerkar.tool.builtins.templates.javabuild.JkJavaBuildPlugin;


@JkDoc({"Add SonarQube capability to a build.",
	"The ananlysis is performed when the 'verify' method is invoked.",
	"To parameterize this plugin just set the relevant sonar properies as options.",
	"For example you can launch the build whith '-sonar.host.url=http://myserver/..' to specify the SonarQube server url."})
public class JkBuildPluginSonar extends JkJavaBuildPlugin {
	
	private final Map<String, String> properties = new HashMap<String, String>();
	
	public static JkSonar configureSonarFrom(JkJavaBuild build) {
		final File baseDir = build.baseDir().root();
		final JkPath libs = build.depsFor(JkJavaBuild.COMPILE, JkJavaBuild.PROVIDED);
		return JkSonar.of(build.moduleId().fullName(), build.moduleId().name(), build.version())
				.withProperties(JkOptions.getAllStartingWith("sonar."))
                .withProjectBaseDir(baseDir) 
                .withBinaries(build.classDir())
                .withLibraries(libs)
                .withSources(build.editedSources().roots())
                .withTest(build.unitTestSources().roots())
                .withProperty(JkSonar.WORKING_DIRECTORY, build.baseDir("build/.sonar").getPath())
                .withProperty(JkSonar.JUNIT_REPORTS_PATH, JkUtilsFile.getRelativePath(baseDir, new File(build.testReportDir(), "junit")))
                .withProperty(JkSonar.SUREFIRE_REPORTS_PATH, JkUtilsFile.getRelativePath(baseDir, new File(build.testReportDir(), "junit")))
                //.withProperty(JkSonar.DYNAMIC_ANALYSIS, "reuseReports")
                .withProperty(JkSonar.JACOCO_REPORTS_PATH, JkUtilsFile.getRelativePath(baseDir, new File(build.testReportDir(), "jacoco/jacoco.exec")));
	}
	
	private JkSonar jkSonar;
	
	@Override
	public void configure(JkBuild build) {
		final JkJavaBuild javaBuild = (JkJavaBuild) build;
		javaBuild.tests.report = JunitReportDetail.FULL;
	    this.jkSonar = configureSonarFrom(javaBuild).withProperties(properties);
	}
	
	@JkDoc("Launch a Sonar analysis.")
	@Override
	public void verify() {
		jkSonar.launch();
	}
	
	public JkBuildPluginSonar prop(String key, String value) {
		this.properties.put(key, value);
		return this;
	}
	

}

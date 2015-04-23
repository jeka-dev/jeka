package org.jerkar.plugins.sonar;

import java.io.File;

import org.jerkar.JkBuild;
import org.jerkar.JkDoc;
import org.jerkar.JkOptions;
import org.jerkar.JkPath;
import org.jerkar.builtins.javabuild.build.JkJavaBuild;
import org.jerkar.builtins.javabuild.build.JkJavaBuildPlugin;
import org.jerkar.builtins.javabuild.testing.junit.JkUnit.JunitReportDetail;
import org.jerkar.utils.JkUtilsFile;

@JkDoc({"Add SonarQube capability to a build.",
	"The ananlysis is performed when the 'verify' method is invoked.",
	"To parameterize this plugin just set the relevant sonar properies as options.",
	"For example you can launch the build whith '-sonar.host.url=http://myserver/..' to specify the SonarQube server url."})
public class JkBuildPluginSonar extends JkJavaBuildPlugin {
	
	public static JkSonar configureSonarFrom(JkJavaBuild build) {
		final File baseDir = build.baseDir().root();
		final JkPath libs = build.depsFor(JkJavaBuild.COMPILE, JkJavaBuild.PROVIDED);
		return JkSonar.of(build.projectFullName(), build.projectName(), build.version())
				.withProperties(JkOptions.getAllStartingWith("sonar."))
                .withProjectBaseDir(baseDir) 
                .withBinaries(build.classDir())
                .withLibraries(libs)
                .withSources(build.editedSourceDirs().roots())
                .withTest(build.testSourceDirs().roots())
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
		javaBuild.junitReportDetail = JunitReportDetail.FULL;
	    this.jkSonar = configureSonarFrom(javaBuild);
	}
	
	@JkDoc("Launch a Sonar analysis.")
	@Override
	public void verify() {
		jkSonar.launch();
	}
	

}

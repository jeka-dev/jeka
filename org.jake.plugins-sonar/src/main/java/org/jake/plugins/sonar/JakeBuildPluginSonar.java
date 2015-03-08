package org.jake.plugins.sonar;

import java.io.File;

import org.jake.JakeBuild;
import org.jake.JakeDoc;
import org.jake.JakePath;
import org.jake.java.build.JakeJavaBuild;
import org.jake.java.build.JakeJavaBuildPlugin;
import org.jake.java.testing.junit.JakeUnit.JunitReportDetail;
import org.jake.utils.JakeUtilsFile;

@JakeDoc({"Run Sonar analysis",
	"When activated this plugin run a Sonar analysis when the build 'verify' method is invoked."})
public class JakeBuildPluginSonar extends JakeJavaBuildPlugin {
	
	public static JakeSonar configureSonarFrom(JakeJavaBuild build) {
		final File baseDir = build.baseDir().root();
		JakePath libs = build.depsFor(JakeJavaBuild.COMPILE, JakeJavaBuild.PROVIDED);
		System.out.println(libs);
		return JakeSonar.of(build.projectFullName(), build.projectName(), build.version())
                .withProjectBaseDir(baseDir)
                .withBinaries(build.classDir())
                .withLibraries(libs)
                .withSources(build.editedSourceDirs().roots())
                .withTest(build.testSourceDirs().roots())
                .withProperty(JakeSonar.WORKING_DIRECTORY, build.baseDir("build/.sonar").getPath())
                .withProperty(JakeSonar.JUNIT_REPORTS_PATH, JakeUtilsFile.getRelativePath(baseDir, new File(build.testReportDir(), "junit")))
                .withProperty(JakeSonar.SUREFIRE_REPORTS_PATH, JakeUtilsFile.getRelativePath(baseDir, new File(build.testReportDir(), "junit")))
                .withProperty(JakeSonar.DYNAMIC_ANALYSIS, "reuseReports")
                .withProperty(JakeSonar.JACOCO_REPORTS_PATH, JakeUtilsFile.getRelativePath(baseDir, new File(build.testReportDir(), "jacoco/jacoco.exec")));
	}
	
	private JakeSonar jakeSonar;
	
	public void configure(JakeBuild build) {
		final JakeJavaBuild javaBuild = (JakeJavaBuild) build;
		javaBuild.junitReportDetail = JunitReportDetail.FULL;
	    this.jakeSonar = configureSonarFrom(javaBuild);
	}
	
	@JakeDoc("Launch a Sonar analysis.")
	@Override
	public void verify() {
		jakeSonar.launch();
	}
	

}

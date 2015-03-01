package org.jake.sonar;

import java.io.File;

import org.jake.JakeLog;
import org.jake.java.build.JakeJavaBuild;
import org.jake.java.build.JakeJavaBuildPlugin;
import org.jake.java.testing.junit.JakeUnit.JunitReportDetail;
import org.jake.utils.JakeUtilsFile;

public class SonarJakeJavaBuildPlugin extends JakeJavaBuildPlugin {
	
	public static JakeSonar configureSonarFrom(JakeJavaBuild build) {
		final File baseDir = build.baseDir().root();
		return JakeSonar.of(build.projectFullName(), build.projectName(), build.version())
                .withProjectBaseDir(baseDir)
                .withBinaries(build.classDir())
                .withLibraries(build.depsFor(JakeJavaBuild.COMPILE))
                .withSources(build.editedSourceDirs().roots())
                .withTest(build.testSourceDirs().roots())
                .withProperty(JakeSonar.WORKING_DIRECTORY, build.baseDir("build/.sonar").getPath())
                .withProperty(JakeSonar.JUNIT_REPORTS_PATH, JakeUtilsFile.getRelativePath(baseDir, new File(build.testReportDir(), "junit")))
                .withProperty(JakeSonar.SUREFIRE_REPORTS_PATH, JakeUtilsFile.getRelativePath(baseDir, new File(build.testReportDir(), "junit")))
                .withProperty(JakeSonar.DYNAMIC_ANALYSIS, "reuseReports")
                .withProperty(JakeSonar.JACOCO_REPORTS_PATH, JakeUtilsFile.getRelativePath(baseDir, new File(build.testReportDir(), "jacoco/jacoco.exec")));
	}
	
	private JakeSonar jakeSonar;
	
	public void configure(JakeJavaBuild build) {
		JakeLog.warnIf(build.junitReportDetail() != JunitReportDetail.FULL,"*  You need to use junitReportDetail=FULL " +
	                "to get complete sonar test report but you are currently using " + build.junitReportDetail().name() + ".");
	    this.jakeSonar = configureSonarFrom(build);
	}
	
	public void run() {
		jakeSonar.launch();
	}
	
	

}

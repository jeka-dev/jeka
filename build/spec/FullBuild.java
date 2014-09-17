import java.io.File;

import org.jake.JakeOptions;
import org.jake.java.JakeJavaProcess;


public class FullBuild extends Build {

	@Override
	protected void runUnitTests() {
		super.runUnitTests();
		final File report = new File(testReportDir(), "jacoco.exec");
		final JakeJavaProcess process = JakeJavaProcess.of().andAgent(this.baseDir("build/libs/jacoco-agent/jacocoagent.jar"),
				"destfile="+ report.getAbsolutePath());
		jakeUnit().forkKeepingSameClassPath(process).launchAll(testClassDir());
	}

	public static void main(String[] args) {
		JakeOptions.forceVerbose(true);
		new FullBuild().base();
	}

}

import org.jake.JakeOptions;
import org.jake.java.test.jacoco.Jakeoco;
import org.jake.java.test.junit.JakeUnit;


public class FullBuild extends Build {

	//	@Override
	//	protected void runUnitTests() {
	//		super.runUnitTests();
	//		final File report = new File(testReportDir(), "jacoco.exec");
	//		final JakeJavaProcess process = JakeJavaProcess.of().andAgent(this.baseDir("build/libs/jacoco-agent/jacocoagent.jar"),
	//				"destfile="+ report.getAbsolutePath());
	//		jakeUnit().forkKeepingSameClassPath(process).launchAll(testClassDir());
	//	}

	@Override
	protected JakeUnit jakeUnit() {
		return Jakeoco.of(this).enhance(super.jakeUnit());
	}

	public static void main(String[] args) {
		JakeOptions.forceVerbose(true);
		new FullBuild().base();
	}

}

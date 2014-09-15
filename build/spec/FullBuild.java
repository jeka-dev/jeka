import org.jake.JakeOptions;
import org.jake.java.JakeJavaProcess;


public class FullBuild extends Build {

	@Override
	protected void runUnitTests() {
		super.runUnitTests();
		jakeUnit().forkKeepingSameClassPath(JakeJavaProcess.of()).launchAll(testClassDir());
	}

	public static void main(String[] args) {
		JakeOptions.forceVerbose(true);
		new FullBuild().base();
	}

}

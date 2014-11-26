import org.jake.JakeJavaCompiler;
import org.jake.JakeOptions;
import org.jake.java.test.junit.JakeUnit;


public class FullBuild extends Build {

	// Add Jacoco agent to the unit test runner.
	@Override
	public JakeUnit unitTester() {
		return super.unitTester().enhancedWith(jacoco());
	}

	// Launch a sonar analysing
	public void sonar() {
		jakeSonar().launch();
	}

	// Use a forked comiler to compile production (non-test) code.
	@Override
	public JakeJavaCompiler productionCompiler() {
		return super.productionCompiler().fork(false);
	}

	// Include sonar analysing in the default method
	@Override
	public void base() {
		super.base();
		sonar();
	}

	public static void main(String[] args) {
		JakeOptions.forceVerbose(true);
		new FullBuild().base();
	}

}

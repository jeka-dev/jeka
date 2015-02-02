package org.jake;
import org.jake.JakeJavaCompiler;
import org.jake.JakeOptions;
import org.jake.java.testing.junit.JakeUnit;

/**
 * Build class for Jake itself.
 * This build does not rely on any dependence manager.
 * This build uses built-in extra feature as sonar, jacoco analysis.
 */
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

	// Use a forked compiler for production (non-test) code.
	@Override
	public JakeJavaCompiler productionCompiler() {
		return super.productionCompiler().fork(false);
	}

	// Include sonar analysis in the default method
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

package org.jake;
import org.jake.java.testing.jacoco.JakeocoJakeJavaBuildPlugin;
import org.jake.java.testing.junit.JakeUnit;

/**
 * Build class for Jake itself.
 * This build does not rely on any dependence manager.
 * This build uses built-in extra feature as sonar, jacoco analysis.
 */
@JakeImport({
	"com.google.guava:guava:18.0",
	"../my.jar",
	"build/lib/extra/mylib.jar"
})
public class FullBuild extends Build {

	// Add Jacoco agent to the unit test runner.
	@Override
	public JakeUnit createUnitTester() {
		return super.unitTester().enhancedWith(JakeocoJakeJavaBuildPlugin.enhancer(this));
	}

	// Use a forked compiler for production (non-test) code.
	@Override
	public JakeJavaCompiler productionCompiler() {
		return super.productionCompiler().fork(false);
	}

	public static void main(String[] args) {
		JakeOptions.forceVerbose(true);
		new FullBuild().base();
	}

}

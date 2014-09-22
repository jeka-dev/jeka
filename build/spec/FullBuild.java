import org.jake.JakeJavaCompiler;
import org.jake.JakeOptions;
import org.jake.java.test.jacoco.Jakeoco;
import org.jake.java.test.junit.JakeUnit;


public class FullBuild extends Build {

	@Override
	public JakeUnit unitTester() {
		return super.unitTester().enhancedWith(Jakeoco.of(this));
	}


	public void sonar() {
		//JakeSonar.of(this).launch();
	}

	@Override
	public JakeJavaCompiler productionCompiler() {
		return super.productionCompiler().fork(false);
	}


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

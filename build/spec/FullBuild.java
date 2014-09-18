import org.jake.JakeOptions;
import org.jake.java.test.jacoco.Jakeoco;
import org.jake.java.test.junit.JakeUnit;
import org.jake.verify.sonar.JakeSonar;


public class FullBuild extends Build {


	@Override
	protected JakeUnit jakeUnit() {
		return Jakeoco.of(this).enhance(super.jakeUnit());
	}

	public void sonar() {
		JakeSonar.of(this).launch();
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

import org.jake.JakeOptions;
import org.jake.java.test.jacoco.Jakeoco;
import org.jake.java.test.junit.JakeUnit;


public class FullBuild extends Build {


	@Override
	protected JakeUnit jakeUnit() {
		return Jakeoco.of(this).enhance(super.jakeUnit());
	}

	public static void main(String[] args) {
		JakeOptions.forceVerbose(true);
		new FullBuild().base();
	}

}

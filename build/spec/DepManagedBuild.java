import org.jake.depmanagement.JakeDependencies;

/**
 * Build class for Jake itself.
 * This build relies on a dependency manager.
 * This build uses built-in extra feature as sonar, jacoco analysis.
 */
public class DepManagedBuild extends Build {

	@Override
	protected JakeDependencies dependencies() {
		return JakeDependencies.builder()
				.forScopes(PROVIDED)
				.on("junit:junit:4.11")
				.on("org.apache.ivy:ivy:2.4.0-rc1").build();
	}

	public static void main(String[] args) {
		//JakeOptions.forceVerbose(true);
		new DepManagedBuild().base();
	}

}

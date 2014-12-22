import org.jake.JakeOptions;
import org.jake.depmanagement.JakeDependencyResolver;
import org.jake.depmanagement.JakeModuleId;
import org.jake.depmanagement.ivy.JakeManagedDependencyResolver;

/**
 * Build class for Jake itself.
 * This build relies on a dependency manager.
 * This build uses built-in extra feature as sonar, jacoco analysis.
 */
public class DepManagedBuild extends Build {

	@Override
	protected JakeDependencyResolver baseDependencyResolver() {
		return JakeDependencyResolver.
	}

	public static void main(String[] args) {
		JakeOptions.forceVerbose(true);
		new DepManagedBuild().base();
	}

}

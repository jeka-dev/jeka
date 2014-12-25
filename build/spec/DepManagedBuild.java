import org.jake.depmanagement.JakeDependencies;
import org.jake.depmanagement.JakeDependencyResolver;
import org.jake.depmanagement.JakeModuleId;
import org.jake.depmanagement.JakeResolutionParameters;
import org.jake.depmanagement.JakeScopeMapping;
import org.jake.depmanagement.JakeVersion;
import org.jake.depmanagement.JakeVersionedModule;
import org.jake.depmanagement.ivy.JakeIvy;
import org.jake.depmanagement.ivy.JakeManagedDependencyResolver;

/**
 * Build class for Jake itself.
 * This build relies on a dependency manager.
 * This build uses built-in extra feature as sonar, jacoco analysis.
 */
public class DepManagedBuild extends Build {

	@Override
	protected JakeDependencyResolver baseDependencyResolver() {
		final JakeVersionedModule module = JakeVersionedModule.of(JakeModuleId.of(groupName(), projectName()), JakeVersion.of(version()));
		final JakeDependencies deps = JakeDependencies.builder()
				.defaultScope(PROVIDED)
				.on("org.apache.ivy:ivy:2.4.0-rc1")
				.on("junit:junit:4.11").build();
		final JakeScopeMapping mapping = JakeScopeMapping
				.of(COMPILE).to("archive(master)", "compile")
				.and(PROVIDED).to("archive(master)")
				.and(RUNTIME).to("archive(master)", "runtime")
				.and(TEST).to("archive(master)", "test");
		return new JakeManagedDependencyResolver(JakeIvy.of(), deps, JakeResolutionParameters.of().withDefault(mapping), module );
	}

	public static void main(String[] args) {
		//JakeOptions.forceVerbose(true);
		new DepManagedBuild().base();
	}

}

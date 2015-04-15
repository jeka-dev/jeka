package org.jerkar;


import org.jerkar.depmanagement.JkDependencies;
import org.jerkar.depmanagement.JkScope;
import org.jerkar.publishing.JkIvyPublication;
import org.jerkar.publishing.JkMavenPublication;

/**
 * Build class for Jerkar itself using managed dependencies.
 */
public class CoreDepManagedBuild extends CoreBuild {

	public static final JkScope DISTRIB = JkScope.of("distrib").descr("Contains Jerkar distribution zip file");

	@Override
	protected JkDependencies dependencies() {
		return JkDependencies.builder()
				.on("junit:junit:4.11").scope(PROVIDED)
				.on("org.apache.ivy:ivy:2.4.0-rc1").build();
	}


	public static void main(String[] args) {
		final CoreDepManagedBuild build = new CoreDepManagedBuild();
		build.doDefault();
	}

	@Override
	protected JkMavenPublication mavenPublication() {
		return super.mavenPublication().and(distripZipFile, DISTRIB.name());
	}

	@Override
	protected JkIvyPublication ivyPublication() {
		return super.ivyPublication().and(distripZipFile, "distrib", DISTRIB);
	}

	@Override
	protected boolean includeTestsInPublication() {
		return true;
	}

}

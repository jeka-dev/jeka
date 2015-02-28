package org.jake;


import org.jake.depmanagement.JakeDependencies;
import org.jake.depmanagement.JakeScope;
import org.jake.publishing.JakeIvyPublication;
import org.jake.publishing.JakeMavenPublication;

/**
 * Build class for Jake itself using managed dependencies.
 */
public class DepManagedBuild extends Build {

	public static final JakeScope DISTRIB = JakeScope.of("distrib").descr("Contains Jake distribution zip file");

	@Override
	protected JakeDependencies dependencies() {
		return JakeDependencies.builder()
				.on("junit:junit:4.11").scope(PROVIDED)
				.on("org.apache.ivy:ivy:2.4.0-rc1").build();
	}


	public static void main(String[] args) {
		final DepManagedBuild build = new DepManagedBuild();
		build.base();
	}

	@Override
	protected JakeMavenPublication mavenPublication() {
		return super.mavenPublication().and(distripZipFile, DISTRIB.name());
	}

	@Override
	protected JakeIvyPublication ivyPublication() {
		return super.ivyPublication().and(distripZipFile, "distrib", DISTRIB);
	}

	@Override
	protected boolean includeTestsInPublication() {
		return true;
	}

}

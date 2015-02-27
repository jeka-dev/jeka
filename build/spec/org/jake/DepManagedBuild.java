package org.jake;


import org.jake.depmanagement.JakeDependencies;
import org.jake.depmanagement.JakeScope;
import org.jake.depmanagement.JakeVersion;
import org.jake.publishing.JakeIvyPublication;
import org.jake.publishing.JakeMavenPublication;
import org.jake.utils.JakeUtilsIterable;

/**
 * Build class for Jake itself.
 * This build relies on a dependency manager.
 * This build uses built-in extra feature as sonar, jacoco analysis.
 */
@JakeImport
({
	"commons-lang:commons-lang:2.6"
})
public class DepManagedBuild extends Build {

	public static final JakeScope DISTRIB = JakeScope.of("distrib").descr("Contains Jake distribution zip file");

	@Override
	protected JakeDependencies dependencies() {
		return JakeDependencies.builder()
				.usingDefaultScopes(PROVIDED)
				.on("junit:junit:4.11")
				.on("org.apache.ivy:ivy:2.4.0-rc1").build();
	}

	@Override
	public void base() {
		super.base();
		doc();
		publish();
		org.apache.commons.lang.ObjectUtils.toString("toto");

	}

	@Override
	public JakeVersion version() {
		return JakeVersion.named("1.0");
	}

	public static void main(String[] args) {
		final DepManagedBuild build = new DepManagedBuild();
		JakeOptions.init(JakeUtilsIterable.mapOf("publishRepoUrl", "ivy:file:///c:/usertemp/i19451/jakerepo-snapshot",
				"publishRepoReleaseUrl", "file:///c:/usertemp/i19451/jakerepo-release"));
		JakeOptions.populateFields(build);
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

	//	@Override
	//	protected JakePublishRepos publishRepositories() {
	//		return JakePublishRepos.maven(baseDir().file("build/output/dummyMavenRerpo"))
	//				.andIvy(JakePublishRepos.ACCEPT_ALL, (baseDir().file("build/output/dummyIvyRerpo")));
	//	}

	@Override
	protected boolean includeTestsInPublication() {
		return true;
	}

}

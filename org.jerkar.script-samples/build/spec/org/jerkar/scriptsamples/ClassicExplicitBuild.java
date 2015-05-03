package org.jerkar.scriptsamples;

import static org.jerkar.builtins.javabuild.JkPopularModules.GUAVA;
import static org.jerkar.builtins.javabuild.JkPopularModules.JERSEY_SERVER;
import static org.jerkar.builtins.javabuild.JkPopularModules.JUNIT;
import static org.jerkar.builtins.javabuild.JkPopularModules.MOCKITO_ALL;

import org.jerkar.JkModuleId;
import org.jerkar.builtins.javabuild.JkJavaBuild;
import org.jerkar.depmanagement.JkDependencies;
import org.jerkar.depmanagement.JkRepo;
import org.jerkar.depmanagement.JkRepos;
import org.jerkar.depmanagement.JkVersion;
import org.jerkar.publishing.JkPublishRepos;

/**
 * Build sample for a jar project depending on several external modules. This
 * build : - produces jar, fat-jar, sources-jar by launching 'jerkar' - produces
 * all this + javadoc by launching 'jerkar doDefault doc' - produces all this +
 * publish on remote repository by typing 'jerkar doDefault doc publish'
 * 
 * @author Jerome Angibaud
 */
public class ClassicExplicitBuild extends JkJavaBuild {

	@Override
	// Optional : needless if you respect naming convention
	public JkModuleId moduleId() {
		return JkModuleId.of("org.jerkar", "script-samples");
	}

	@Override
	// Optional : needless if you get the version from your SCM or version.txt
	// resource
	protected JkVersion defaultVersion() {
		return JkVersion.ofName("0.3-SNAPSHOT");
	}

	@Override
	// Optional : needless if you use only local dependencies
	protected JkDependencies dependencies() {
		return JkDependencies
				.builder()
				.on(GUAVA, "18.0")
				// Popular modules are available as Java constant
				.on(JERSEY_SERVER, "1.19")
				.on("com.orientechnologies:orientdb-client:2.0.8")
				.on(JUNIT, "4.11").scope(TEST).on(MOCKITO_ALL, "1.9.5")
				.scope(TEST).build();
	}
	
	@Override
	protected JkRepos downloadRepositories() {
		return JkRepos.of(JkRepo.maven("http://my.repo1"), JkRepo.mavenCentral());
	}
	
	@Override
	protected JkPublishRepos publishRepositories() {
		return JkPublishRepos.ofSnapshotAndRelease(
				JkRepo.maven("http://my.snapshot.repo"), 
				JkRepo.ivy("http://my.release.repo"));
	}

	// Optional : usefull if you want quick run/debug your script in you IDE
	public static void main(String[] args) {
		ClassicExplicitBuild build = new ClassicExplicitBuild();
		build.doDefault();
		build.doc();
		build.publish();
	}

}

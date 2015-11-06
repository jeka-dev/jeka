package org.jerkar.samples;

import static org.jerkar.api.depmanagement.JkPopularModules.GUAVA;
import static org.jerkar.api.depmanagement.JkPopularModules.JERSEY_SERVER;
import static org.jerkar.api.depmanagement.JkPopularModules.JUNIT;
import static org.jerkar.api.depmanagement.JkPopularModules.MOCKITO_ALL;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkIvyPublication;
import org.jerkar.api.depmanagement.JkModuleId;
import org.jerkar.api.depmanagement.JkPublishRepos;
import org.jerkar.api.depmanagement.JkRepo;
import org.jerkar.api.depmanagement.JkRepos;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;

/**
 * Build sample for a jar project depending on several external modules. This
 * build : - produces jar, fat-jar, sources-jar by launching 'jerkar' - produces
 * all this + javadoc by launching 'jerkar doDefault doc' - produces all this +
 * publish on remote repository by typing 'jerkar doDefault doc publish'
 * 
 * @author Jerome Angibaud
 */
public class IvyPublishBuild extends JkJavaBuild {
	
	{
		this.pack.tests = true;
		this.pack.javadoc = true;
	}

	
	
	@Override
	// Optional : needless if you respect naming convention
	public JkModuleId moduleId() {
		return JkModuleId.of("org.jerkar", "script-samples-ivy");
	}

	@Override
	public JkDependencies dependencies() {
		return JkDependencies.builder()
				.on(GUAVA, "18.0")	
				.on(JERSEY_SERVER, "1.19").mapScope(RUNTIME, TEST).to(COMPILE)
				.on("com.orientechnologies:orientdb-client:2.0.8")
				.on(JUNIT, "4.11").scope(TEST)
				.on(MOCKITO_ALL, "1.9.5").scope(TEST)
				.build();
	}
	
	@Override
	protected JkRepos downloadRepositories() {
		return JkRepo.ivy(this.repo.publish.url).and(JkRepo.mavenCentral());
	}
	
	@Override 
	protected JkPublishRepos publishRepositories() {
		return JkRepo.ivy(this.repo.publish.url).asPublishRepos();
	}
	
	@Override
	protected JkIvyPublication ivyPublication() {
		return JkIvyPublication.of(packer().jarFile(), COMPILE)
				.and(packer().jarTestFile(), "test", TEST)
				.and(packer().javadocFile(),"javadoc", JAVADOC)
				.and(packer().jarSourceFile(), "source", SOURCES)
				.and(packer().jarTestSourceFile(), "test-source", SOURCES, TEST);
	}

}

package org.jerkar.api.depmanagement;



import static org.jerkar.api.depmanagement.JkScopedDependencyTest.COMPILE;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.jerkar.api.system.JkLog;

public class IvyResolverRunner {

	static final JkRepos REPOS = JkRepos.maven("http://i-net1102e-prod:8081/nexus/content/groups/bnppf-secured/");

	//private static final JkRepos REPOS = JkRepos.mavenCentral();

	public static void main(String[] args) {
		JkLog.verbose(false);
		//spring();
		hibernate();
		//jogl();
		//joglWithSource();
		//testPublishIvy();
	}

	public static void spring() {
		final JkRepos repos = REPOS;
		//JkRepos repos = JkRepos.mavenCentral();
		final JkDependencies deps = JkDependencies.builder()
				.on("org.springframework", "spring-jdbc", "3.0.+")  // This works on nexus repo
				.mapScope(COMPILE).to("compile", "default")
				.and(JkScopedDependencyTest.PROVIDED).to("provided")
				.excludeGlobally("org.springframework","spring-core")
				.build();
		final JkResolutionParameters params = JkResolutionParameters.of();
		final JkResolveResult resolveResult = IvyResolver.of(repos).resolveAnonymous(deps, COMPILE, params);
		for (final File file : resolveResult.localFiles()) {
			System.out.println(file.getAbsolutePath());
		}
		System.out.println(deps.resolvedWith(resolveResult.involvedModules()));
	}

	public static void hibernate() {
		final JkDependencies deps = JkDependencies.builder()
				.on("org.hibernate:hibernate-core:4.3.7.Final")
				.excludeLocally("org.jboss.logging","*")
				.excludeLocally("antlr","*")
				.scope(COMPILE)
				.excludeGlobally("dom4j","*")
				.build();
		final JkResolveResult resolveResult = IvyResolver.of(REPOS).resolveAnonymous(deps, COMPILE, JkResolutionParameters.of());
		for (final File file : resolveResult.localFiles()) {
			System.out.println(file.getAbsolutePath());
		}
		System.out.println(deps.resolvedWith(resolveResult.involvedModules()));
	}


	public static void jogl() {
		final JkRepos repos = JkRepos.mavenCentral().andMavenCentral();
		final JkDependencies deps = JkDependencies.builder()
				.on("org.apache.cocoon.all:cocoon-all:3.0.0-alpha-3").scope(COMPILE).build();
		final JkResolveResult resolveResult = IvyResolver.of(repos).resolveAnonymous(deps, COMPILE, JkResolutionParameters.of().withDefault(defaultMapping()));
		for (final File file : resolveResult.localFiles()) {
			System.out.println(file.getAbsolutePath());
		}
		System.out.println("--- " + resolveResult.localFiles().size());
	}

	private static JkScopeMapping defaultMapping() {
		return JkScopeMapping.of(COMPILE).to("compile", "archive(master)")
				.and(JkScopedDependencyTest.RUNTIME).to("runtime", "archive(master)");
	}

	public static void joglWithSource() {
		final JkRepos repos = JkRepos.mavenCentral();
		final JkDependencies deps = JkDependencies.builder()
				.on("org.apache.cocoon.all:cocoon-all:3.0.0-alpha-3").scope(COMPILE).build();
		final InternalDepResolver jkIvyResolver = IvyResolver.of(repos);
		final JkResolveResult resolveResult = jkIvyResolver.resolveAnonymous(deps, COMPILE, JkResolutionParameters.of().withDefault(defaultMapping()));
		final Set<JkVersionedModule> modules = new HashSet<JkVersionedModule>();
		for (final JkVersionedModule versionedModule : resolveResult.involvedModules()) {
			modules.add(versionedModule);
		}
		final JkAttachedArtifacts result = jkIvyResolver.getArtifacts(modules, JkScope.of("sources"), JkScope.of("javadoc"), JkScope.of("noexist"));
		System.out.println(result);
		final Set<JkModuleDepFile> artifactSet = result.getArtifacts(JkModuleId.of("org.apache.wicket", "wicket-ioc"), JkScope.of("sources"));
		System.out.println(artifactSet);
		final Set<JkModuleDepFile> javadocArtifactSet = result.getArtifacts(JkModuleId.of("org.apache.wicket", "wicket-ioc"), JkScope.of("javadoc"));
		System.out.println(javadocArtifactSet);
		final Set<JkModuleDepFile> noExistArtifactSet = result.getArtifacts(JkModuleId.of("org.apache.wicket", "wicket-ioc"), JkScope.of("noexist"));
		System.out.println(noExistArtifactSet);
	}




}

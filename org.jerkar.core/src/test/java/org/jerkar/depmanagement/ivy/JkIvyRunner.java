package org.jerkar.depmanagement.ivy;

import static org.jerkar.builtins.javabuild.JkJavaBuild.COMPILE;
import static org.jerkar.builtins.javabuild.JkJavaBuild.PROVIDED;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.jerkar.JkOptions;
import org.jerkar.JkModuleId;
import org.jerkar.builtins.javabuild.JkJavaBuild;
import org.jerkar.depmanagement.JkArtifact;
import org.jerkar.depmanagement.JkDependencies;
import org.jerkar.depmanagement.JkRepo;
import org.jerkar.depmanagement.JkRepos;
import org.jerkar.depmanagement.JkResolutionParameters;
import org.jerkar.depmanagement.JkScope;
import org.jerkar.depmanagement.JkScopeMapping;
import org.jerkar.depmanagement.JkVersion;
import org.jerkar.depmanagement.JkVersionedModule;
import org.jerkar.depmanagement.ivy.JkIvy.AttachedArtifacts;
import org.jerkar.publishing.JkIvyPublication;
import org.jerkar.publishing.JkMavenPublication;
import org.jerkar.utils.JkUtilsFile;

public class JkIvyRunner {

	public static void main(String[] args) {
		JkOptions.forceVerbose(false);
		//spring();
		//jogl();
		//joglWithSource();
		//testPublishIvy();
		testPublishMaven();
	}

	public static void spring() {
		final JkRepos repos = JkRepos.maven("http://i-net1102e-prod:8081/nexus/content/groups/bnppf-secured").andMavenCentral();
		final JkDependencies deps = JkDependencies.builder()
				.on("org.springframework", "spring-jdbc", "3.0.+")
				.mapScope(COMPILE).to("compile", "default")
				.and(PROVIDED).to("provided")
				.build();
		final Set<JkArtifact> artifacts = JkIvy.of(repos).resolve(deps, COMPILE);
		for (final JkArtifact artifact : artifacts) {
			System.out.println(artifact);
		}
		System.out.println(deps.resolvedWithArtifacts(artifacts));
	}

	public static void jogl() {
		final JkRepos repos = JkRepos.maven("http://i-net1102e-prod:8081/nexus/content/groups/bnppf-secured")
				.andMavenCentral();
		final JkDependencies deps = JkDependencies.builder()
				.on("org.apache.cocoon.all:cocoon-all:3.0.0-alpha-3").scope(COMPILE).build();
		final Set<JkArtifact> artifacts = JkIvy.of(repos).resolve(deps, COMPILE, JkResolutionParameters.of(defaultMapping()));
		for (final JkArtifact artifact : artifacts) {
			System.out.println(artifact);
		}
		System.out.println("--- " + artifacts.size());
	}

	private static JkScopeMapping defaultMapping() {
		return JkScopeMapping.of(COMPILE).to("compile", "archive(master)")
				.and(JkJavaBuild.RUNTIME).to("runtime", "archive(master)");
	}

	public static void joglWithSource() {
		final JkRepos repos = JkRepos.maven("http://i-net1102e-prod:8081/nexus/content/groups/bnppf-secured")
				.andMavenCentral();
		final JkDependencies deps = JkDependencies.builder()
				.on("org.apache.cocoon.all:cocoon-all:3.0.0-alpha-3").scope(COMPILE).build();
		final JkIvy jkIvy = JkIvy.of(repos);
		final Set<JkArtifact> artifacts = jkIvy.resolve(deps, COMPILE, JkResolutionParameters.of(defaultMapping()));
		final Set<JkVersionedModule> modules = new HashSet<JkVersionedModule>();
		for (final JkArtifact artifact : artifacts) {
			modules.add(artifact.versionedModule());
		}
		final AttachedArtifacts result = jkIvy.getArtifacts(modules, JkScope.of("sources"), JkScope.of("javadoc"), JkScope.of("noexist"));
		System.out.println(result);
		final Set<JkArtifact> artifactSet = result.getArtifacts(JkModuleId.of("org.apache.wicket", "wicket-ioc"), JkScope.of("sources"));
		System.out.println(artifactSet);
		final Set<JkArtifact> javadocArtifactSet = result.getArtifacts(JkModuleId.of("org.apache.wicket", "wicket-ioc"), JkScope.of("javadoc"));
		System.out.println(javadocArtifactSet);
		final Set<JkArtifact> noExistArtifactSet = result.getArtifacts(JkModuleId.of("org.apache.wicket", "wicket-ioc"), JkScope.of("noexist"));
		System.out.println(noExistArtifactSet);
	}

	public static void testPublishIvy() {
		final JkIvy jkIvy = JkIvy.of(JkRepos.of(ivyRepo()).andMaven("http://i-net1102e-prod:8081/nexus/content/groups/bnppf-secured"));
		final JkVersionedModule versionedModule = JkVersionedModule.of(JkModuleId.of("mygroup", "mymodule"), JkVersion.ofName("myVersion"));
		final JkIvyPublication ivyPublication = JkIvyPublication.of(sampleJarfile(), COMPILE, JkJavaBuild.TEST);
		final JkDependencies deps = JkDependencies.builder()
				.on("org.springframework", "spring-jdbc", "3.0.+").scope(COMPILE).build();
		jkIvy.publishToIvyRepo(versionedModule, ivyPublication,deps, null, null, new Date());
	}

	public static void testPublishMaven() {
		final JkIvy jkIvy = JkIvy.of(JkRepos.of(mavenRepo()).andMaven("http://i-net1102e-prod:8081/nexus/content/groups/bnppf-secured"));
		final JkVersionedModule versionedModule = JkVersionedModule.of(JkModuleId.of("mygroup2", "mymodule2"), JkVersion.ofName("0.0.1"));
		final JkMavenPublication publication = JkMavenPublication.of("mymodule2", sampleJarfile()).and(sampleJarSourcefile(), "source");
		final JkDependencies deps = JkDependencies.builder()
				.on("org.springframework", "spring-jdbc", "3.0.+").scope(COMPILE).build();
		jkIvy.publishToMavenRepo(versionedModule, publication,deps, new Date());
	}


	private static File sampleJarfile() {
		final URL url = JkIvyRunner.class.getResource("myArtifactSample.jar");
		try {
			return new File(url.toURI().getPath());
		} catch (final URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	private static File sampleJarSourcefile() {
		final URL url = JkIvyRunner.class.getResource("myArtifactSample-source.jar");
		try {
			return new File(url.toURI().getPath());
		} catch (final URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	private static JkRepo.JkIvyRepository ivyRepo() {
		final File baseDir = new File(JkUtilsFile.workingDir(), "build/output/testIvyRepo");
		baseDir.mkdir();
		return JkRepo.ivy(baseDir);
	}

	private static JkRepo mavenRepo() {
		final File baseDir = new File(JkUtilsFile.workingDir(), "build/output/mavenRepo");
		baseDir.mkdir();
		return JkRepo.maven(baseDir);
	}


}

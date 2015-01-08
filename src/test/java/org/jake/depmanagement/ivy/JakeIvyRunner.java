package org.jake.depmanagement.ivy;

import static org.jake.java.build.JakeBuildJava.COMPILE;
import static org.jake.java.build.JakeBuildJava.PROVIDED;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.jake.JakeOptions;
import org.jake.depmanagement.JakeArtifact;
import org.jake.depmanagement.JakeDependencies;
import org.jake.depmanagement.JakeModuleId;
import org.jake.depmanagement.JakeRepo;
import org.jake.depmanagement.JakeRepos;
import org.jake.depmanagement.JakeResolutionParameters;
import org.jake.depmanagement.JakeScope;
import org.jake.depmanagement.JakeScopeMapping;
import org.jake.depmanagement.JakeVersion;
import org.jake.depmanagement.JakeVersionedModule;
import org.jake.depmanagement.ivy.JakeIvy.AttachedArtifacts;
import org.jake.java.build.JakeBuildJava;
import org.jake.publishing.JakeIvyPublication;
import org.jake.utils.JakeUtilsFile;

public class JakeIvyRunner {

	public static void main(String[] args) {
		JakeOptions.forceVerbose(false);
		//spring();
		//jogl();
		//joglWithSource();
		testPublishMaven();
	}

	public static void spring() {
		final JakeRepos repos = JakeRepos.maven("http://i-net1102e-prod:8081/nexus/content/groups/bnppf-secured").andMavenCentral();
		final JakeDependencies deps = JakeDependencies.builder()
				.on("org.springframework", "spring-jdbc", "3.0.+")
				.mapScope(COMPILE).to("compile", "default")
				.and(PROVIDED).to("provided")
				.build();
		final Set<JakeArtifact> artifacts = JakeIvy.of(repos).resolve(deps, COMPILE);
		for (final JakeArtifact artifact : artifacts) {
			System.out.println(artifact);
		}
		System.out.println(deps.resolvedWithArtifacts(artifacts));
	}

	public static void jogl() {
		final JakeRepos repos = JakeRepos.maven("http://i-net1102e-prod:8081/nexus/content/groups/bnppf-secured")
				.andMavenCentral();
		final JakeDependencies deps = JakeDependencies.builder()
				.on("org.apache.cocoon.all:cocoon-all:3.0.0-alpha-3").scope(COMPILE).build();
		final Set<JakeArtifact> artifacts = JakeIvy.of(repos).resolve(deps, COMPILE, JakeResolutionParameters.of(defaultMapping()));
		for (final JakeArtifact artifact : artifacts) {
			System.out.println(artifact);
		}
		System.out.println("--- " + artifacts.size());
	}

	private static JakeScopeMapping defaultMapping() {
		return JakeScopeMapping.of(COMPILE).to("compile", "archive(master)")
				.and(JakeBuildJava.RUNTIME).to("runtime", "archive(master)");
	}

	public static void joglWithSource() {
		final JakeRepos repos = JakeRepos.maven("http://i-net1102e-prod:8081/nexus/content/groups/bnppf-secured")
				.andMavenCentral();
		final JakeDependencies deps = JakeDependencies.builder()
				.on("org.apache.cocoon.all:cocoon-all:3.0.0-alpha-3").scope(COMPILE).build();
		final JakeIvy jakeIvy = JakeIvy.of(repos);
		final Set<JakeArtifact> artifacts = jakeIvy.resolve(deps, COMPILE, JakeResolutionParameters.of(defaultMapping()));
		final Set<JakeVersionedModule> modules = new HashSet<JakeVersionedModule>();
		for (final JakeArtifact artifact : artifacts) {
			modules.add(artifact.versionedModule());
		}
		final AttachedArtifacts result = jakeIvy.getArtifacts(modules, JakeScope.of("sources"), JakeScope.of("javadoc"), JakeScope.of("noexist"));
		System.out.println(result);
		final Set<JakeArtifact> artifactSet = result.getArtifacts(JakeModuleId.of("org.apache.wicket:wicket-ioc"), JakeScope.of("sources"));
		System.out.println(artifactSet);
		final Set<JakeArtifact> javadocArtifactSet = result.getArtifacts(JakeModuleId.of("org.apache.wicket:wicket-ioc"), JakeScope.of("javadoc"));
		System.out.println(javadocArtifactSet);
		final Set<JakeArtifact> noExistArtifactSet = result.getArtifacts(JakeModuleId.of("org.apache.wicket:wicket-ioc"), JakeScope.of("noexist"));
		System.out.println(noExistArtifactSet);
	}

	public static void testPublish() {
		final JakeIvy jakeIvy = JakeIvy.of(JakeRepos.of(ivyRepo()).andMaven("http://i-net1102e-prod:8081/nexus/content/groups/bnppf-secured"));
		final JakeVersionedModule versionedModule = JakeVersionedModule.of(JakeModuleId.of("mygroup:mymodule"), JakeVersion.of("myVersion"));
		final JakeIvyPublication ivyPublication = JakeIvyPublication.of(sampleJarfile(), COMPILE, JakeBuildJava.TEST);
		final JakeDependencies deps = JakeDependencies.builder()
				.on("org.springframework", "spring-jdbc", "3.0.+").scope(COMPILE).build();
		jakeIvy.publish(versionedModule, ivyPublication,deps, null, null, new Date());
	}

	public static void testPublishMaven() {
		final JakeIvy jakeIvy = JakeIvy.of(JakeRepos.of(mavenRepo()).andMaven("http://i-net1102e-prod:8081/nexus/content/groups/bnppf-secured"));
		final JakeVersionedModule versionedModule = JakeVersionedModule.of(JakeModuleId.of("mygroup:mymodule"), JakeVersion.of("myVersion"));
		final JakeIvyPublication ivyPublication = JakeIvyPublication.of(sampleJarfile(), COMPILE, JakeBuildJava.TEST);
		final JakeDependencies deps = JakeDependencies.builder()
				.on("org.springframework", "spring-jdbc", "3.0.+").scope(COMPILE).build();
		jakeIvy.publish(versionedModule, ivyPublication,deps, null, null, new Date());
	}


	private static File sampleJarfile() {
		final URL url = JakeIvyRunner.class.getResource("myArtifactSample.jar");
		try {
			return new File(url.toURI().getPath());
		} catch (final URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	private static JakeRepo.IvyRepository ivyRepo() {
		final File baseDir = new File(JakeUtilsFile.workingDir(), "build/output/testIvyRepo");
		baseDir.mkdir();
		return JakeRepo.ivy(baseDir);
	}

	private static JakeRepo mavenRepo() {
		final File baseDir = new File(JakeUtilsFile.workingDir(), "build/output/mavenRepo");
		baseDir.mkdir();
		return JakeRepo.maven(baseDir);
	}


}

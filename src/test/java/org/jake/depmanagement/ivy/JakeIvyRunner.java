package org.jake.depmanagement.ivy;

import static org.jake.java.build.JakeBuildJava.COMPILE;
import static org.jake.java.build.JakeBuildJava.PROVIDED;

import java.util.Set;

import org.jake.JakeOptions;
import org.jake.depmanagement.JakeArtifact;
import org.jake.depmanagement.JakeDependencies;
import org.jake.depmanagement.JakeRepos;
import org.jake.depmanagement.JakeResolutionParameters;
import org.jake.depmanagement.JakeScopeMapping;
import org.jake.java.build.JakeBuildJava;

public class JakeIvyRunner {

	public static void main(String[] args) {
		JakeOptions.forceVerbose(false);
		//spring();
		jogl();
	}

	public static void spring() {
		final JakeRepos repos = JakeRepos.maven("http://i-net1102e-prod:8081/nexus/content/groups/bnppf-secured").andMavenCentral();
		final JakeDependencies deps = JakeDependencies.builder()
				.on("org.springframework", "spring-jdbc", "3.0.0.RELEASE")
				.mapScope(COMPILE).to("compile", "default")
				.and(PROVIDED).to("provided")
				.build();
		final Set<JakeArtifact> artifacts = JakeIvy.of(repos).resolve(deps, COMPILE);
		for (final JakeArtifact artifact : artifacts) {
			System.out.println(artifact);
		}
	}

	public static void jogl() {
		final JakeRepos repos = JakeRepos.maven("http://i-net1102e-prod:8081/nexus/content/groups/bnppf-secured")
				.andMavenCentral();
		final JakeDependencies deps = JakeDependencies.builder()
				.on("org.apache.cocoon.all:cocoon-all:3.0.0-alpha-3").scope(COMPILE).build();
		final Set<JakeArtifact> artifacts = JakeIvy.of(repos).resolve(deps, JakeResolutionParameters.resolvedScopes(COMPILE).withDefault(defaultMapping()));
		for (final JakeArtifact artifact : artifacts) {
			System.out.println(artifact);
		}
		System.out.println("--- " + artifacts.size());
	}

	private static JakeScopeMapping defaultMapping() {
		return JakeScopeMapping.of(COMPILE).to("compile", "archive(master)")
				.and(JakeBuildJava.RUNTIME).to("runtime", "archive(master)");
	}

}

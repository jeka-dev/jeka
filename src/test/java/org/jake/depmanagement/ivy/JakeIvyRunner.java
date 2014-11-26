package org.jake.depmanagement.ivy;

import static org.jake.java.build.JakeBuildJava.COMPILE;
import static org.jake.java.build.JakeBuildJava.PROVIDED;

import java.util.List;

import org.jake.JakeOptions;
import org.jake.depmanagement.JakeArtifact;
import org.jake.depmanagement.JakeDependencies;
import org.jake.depmanagement.JakeExternalModule;
import org.jake.depmanagement.JakeRepos;
import org.jake.depmanagement.JakeResolutionScope;
import org.jake.depmanagement.JakeScopeMapping;
import org.jake.java.build.JakeBuildJava;

public class JakeIvyRunner {

	public static void main(String[] args) {
		JakeOptions.forceVerbose(true);
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
		final List<JakeArtifact> artifacts = JakeIvy.of(repos).resolve(deps, COMPILE);
		for (final JakeArtifact artifact : artifacts) {
			System.out.println(artifact);
		}
	}

	public static void jogl() {
		final JakeRepos repos = JakeRepos.maven("http://i-net1102e-prod:8081/nexus/content/groups/bnppf-secured")
				.andMavenCentral();
		final JakeDependencies deps = JakeDependencies.builder()
				.on(JakeExternalModule.of("org.jogamp.jogl", "jogl-all", "2.0-rc11")
						.classifier("natives-solaris-i586")).scope(COMPILE)
						.build();
		final List<JakeArtifact> artifacts = JakeIvy.of(repos).resolve(deps, JakeResolutionScope.of(COMPILE).withDefault(defaultMapping()));
		for (final JakeArtifact artifact : artifacts) {
			System.out.println(artifact);
		}
	}

	private static JakeScopeMapping defaultMapping() {
		return JakeScopeMapping.of(COMPILE, "compile", "master")
				.and(JakeBuildJava.RUNTIME, "runtime", "master");
	}

}

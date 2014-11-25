package org.jake.depmanagement.ivy;

import static org.jake.java.build.JakeBuildJava.COMPILE;

import java.util.List;

import org.jake.JakeOptions;
import org.jake.depmanagement.JakeArtifact;
import org.jake.depmanagement.JakeDependencies;
import org.jake.depmanagement.JakeExternalModule;
import org.jake.depmanagement.JakeRepos;
import org.jake.depmanagement.JakeScope;
import org.jake.depmanagement.JakeScopeMapping;

public class JakeIvyRunner {

	public static void main(String[] args) {
		spring();
		//jogl();
	}

	public static void spring() {
		JakeOptions.forceVerbose(false);
		final JakeRepos repos = JakeRepos.maven("http://i-net1102e-prod:8081/nexus/content/groups/bnppf-secured").andMavenCentral();
		final JakeDependencies deps = JakeDependencies.builder()
				.defaultScope(COMPILE)
				.on("org.springframework", "spring-jdbc", "3.0.0.RELEASE")
				.build();
		final List<JakeArtifact> artifacts = JakeIvy.of(repos).resolve(deps);
		for (final JakeArtifact artifact : artifacts) {
			System.out.println(artifact);
		}
	}

	public static void jogl() {
		JakeOptions.forceVerbose(false);
		final JakeRepos repos = JakeRepos.maven("http://i-net1102e-prod:8081/nexus/content/groups/bnppf-secured").andMavenCentral();
		final JakeDependencies deps = JakeDependencies.builder()
				.on(JakeExternalModule.of("org.jogamp.jogl", "jogl-all", "2.0-rc11")
						.withMavenClassifier("natives-solaris-i586")).scope(JakeScopeMapping.of(JakeScope.of("default"), COMPILE))
						.build();
		final List<JakeArtifact> artifacts = JakeIvy.of(repos).resolve(deps, COMPILE);
		for (final JakeArtifact artifact : artifacts) {
			System.out.println(artifact);
		}
	}

}

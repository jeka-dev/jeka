package org.jake.ivy;

import static org.jake.java.build.JakeBuildJava.COMPILE;

import java.util.List;

import org.jake.JakeOptions;
import org.jake.depmanagement.JakeArtifact;
import org.jake.depmanagement.JakeDependencies;
import org.jake.depmanagement.JakeRepos;
import org.jake.depmanagement.JakeScope;
import org.jake.depmanagement.JakeScopeMapping;
import org.jake.depmanagement.ivy.JakeIvy;

public class JakeIvyRunner {

	public static void main(String[] args) {
		JakeOptions.forceVerbose(false);
		final JakeRepos repos = JakeRepos.maven("http://i-net1102e-prod:8081/nexus/content/groups/bnppf-secured").andMavenCentral();
		final JakeDependencies deps = JakeDependencies.builder()
				.on("org.springframework", "spring-jdbc", "3.0.0.RELEASE").scope(JakeScopeMapping.of(JakeScope.of("default"), COMPILE))
				.build();
		final List<JakeArtifact> artifacts = JakeIvy.of(repos).resolve(deps, JakeScope.of("default"));
		for (final JakeArtifact artifact : artifacts) {
			System.out.println(artifact);
		}
	}

}

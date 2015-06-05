package org.jerkar.internal.ivy;

import static org.jerkar.builtins.javabuild.JkJavaBuild.COMPILE;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;

import org.jerkar.JkOptions;
import org.jerkar.builtins.javabuild.JkJavaBuild;
import org.jerkar.depmanagement.JkDependencies;
import org.jerkar.depmanagement.JkModuleId;
import org.jerkar.depmanagement.JkVersion;
import org.jerkar.depmanagement.JkVersionedModule;
import org.jerkar.publishing.JkIvyPublication;
import org.jerkar.publishing.JkMavenPublication;
import org.jerkar.publishing.JkPublishRepos;
import org.jerkar.utils.JkUtilsFile;

public class JkIvyPublisherRunner {

	public static void main(String[] args) {
		JkOptions.forceVerbose(false);
		testPublishMaven();
	}


	public static void testPublishIvy() {
		final JkIvyPublisher jkIvyResolver = JkIvyPublisher.of(ivyRepos(), new File("build/output/test-out"));
		final JkVersionedModule versionedModule = JkVersionedModule.of(JkModuleId.of("mygroup", "mymodule"), JkVersion.ofName("myVersion"));
		final JkIvyPublication ivyPublication = JkIvyPublication.of(sampleJarfile(), COMPILE, JkJavaBuild.TEST);
		final JkDependencies deps = JkDependencies.builder()
				.on("org.springframework", "spring-jdbc", "3.0.+").scope(COMPILE).build();
		jkIvyResolver.publishToIvyRepo(versionedModule, ivyPublication,deps, null, null, new Date());
	}

	public static void testPublishMaven() {
		final JkIvyPublisher jkIvyResolver = JkIvyPublisher.of(mavenRepos(), new File("build/output/test-out"));
		final JkVersionedModule versionedModule = JkVersionedModule.of(JkModuleId.of("mygroup2", "mymodule2"), JkVersion.ofName("0.0.1"));
		final JkMavenPublication publication = JkMavenPublication.of("mymodule2", sampleJarfile()).and(sampleJarSourcefile(), "source");
		final JkDependencies deps = JkDependencies.builder()
				.on("org.springframework", "spring-jdbc", "3.0.+").scope(COMPILE).build();
		jkIvyResolver.publishToMavenRepo(versionedModule, publication,deps, new Date());
	}


	private static File sampleJarfile() {
		final URL url = JkIvyPublisherRunner.class.getResource("myArtifactSample.jar");
		try {
			return new File(url.toURI().getPath());
		} catch (final URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	private static File sampleJarSourcefile() {
		final URL url = JkIvyPublisherRunner.class.getResource("myArtifactSample-source.jar");
		try {
			return new File(url.toURI().getPath());
		} catch (final URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	private static JkPublishRepos ivyRepos() {
		final File baseDir = new File(JkUtilsFile.workingDir(), "build/output/testIvyRepo");
		baseDir.mkdir();
		return JkPublishRepos.ivy(baseDir);
	}

	private static JkPublishRepos mavenRepos() {
		final File baseDir = new File(JkUtilsFile.workingDir(), "build/output/mavenRepo");
		baseDir.mkdir();
		return JkPublishRepos.maven(baseDir);
	}


}

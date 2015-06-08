package org.jerkar;

import org.jerkar.builtins.javabuild.JkJavaBuild;
import org.jerkar.depmanagement.JkVersion;
import org.jerkar.publishing.JkMavenPublication;
import org.jerkar.publishing.JkMavenPublicationInfo;
import org.jerkar.publishing.JkPublishRepos;

/**
 * Build settings shared across all Jerkar java projects
 */
public abstract class JerkarBuild extends JkJavaBuild {


	@JkOption("do or skip javadoc")
	public boolean doJavadoc = true;


	@Override
	public String sourceJavaVersion() {
		return JkJavaCompiler.V6;
	}

	@Override
	protected JkVersion defaultVersion() {
		return JkVersion.ofName("0.1-SNAPSHOT");
	}

	@Override
	protected JkMavenPublication mavenPublication() {
		return super.mavenPublication().with(
				JkMavenPublicationInfo
				.of("Jerkar", "Build simpler, stronger, faster", "http://jerkar.github.io")
				.withScm("https://github.com/jerkar/jerkar.git")
				.andApache2License()
				.andGitHubDeveloper("djeang", "djeangdev@yahoo.fr")
				);
	}

	@Override
	public void pack() {
		super.pack();
		if (doJavadoc) {
			javadoc();
		}
	}

	@Override  // Force to use OSSRH
	protected JkPublishRepos publishRepositories() {
		return JkPublishRepos.ossrh(this.repo.publish.username, this.repo.publish.password);
	}

}

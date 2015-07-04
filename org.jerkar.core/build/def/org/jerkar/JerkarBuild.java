package org.jerkar;

import org.jerkar.api.depmanagement.JkMavenPublication;
import org.jerkar.api.depmanagement.JkMavenPublicationInfo;
import org.jerkar.api.depmanagement.JkPublishRepos;
import org.jerkar.api.depmanagement.JkVersion;
import org.jerkar.api.java.JkJavaCompiler;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.JkOptions;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;

/**
 * Build settings shared across all Jerkar java projects
 */
public abstract class JerkarBuild extends JkJavaBuild {


	@JkDoc("do or skip javadoc")
	public boolean doJavadoc = true;


	@Override
	public String sourceJavaVersion() {
		return JkJavaCompiler.V6;
	}

	@Override
	protected JkVersion version() {
		return JkVersion.ofName("0.1.2-SNAPSHOT");
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
		return JkPublishRepos.ossrh(JkOptions.get("ossrh.username"),
				JkOptions.get("ossrh.password"), pgp());
	}

}

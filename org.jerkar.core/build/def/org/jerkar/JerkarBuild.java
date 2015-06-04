package org.jerkar;

import org.jerkar.builtins.javabuild.JkJavaBuild;
import org.jerkar.depmanagement.JkRepo.JkMavenRepository;
import org.jerkar.depmanagement.JkVersion;
import org.jerkar.publishing.JkMavenPublication;
import org.jerkar.publishing.JkMavenPublicationInfo;

public abstract class JerkarBuild extends JkJavaBuild {

	public boolean doJavadoc = true;

	@Override
	protected void init() {
		super.init();
		this.repo.publish.url = JkMavenRepository.MAVEN_OSSRH_PUSH_SNAPSHOT_AND_PULL.toExternalForm();
		this.repo.release.url = JkMavenRepository.MAVEN_OSSRH_PUSH_RELEASE.toExternalForm();
	}

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
				.of("Jerkar", "Build simpler, stronger, faster", "https://github.com/jerkar")
				.withScm("https://github.com/jerkar/jerkar.git")
				.andApache2License()
				.andGitHubDeveloper("djeang", "dgeangdev@yahoo.fr")
				);
	}

	@Override
	public void pack() {
		super.pack();
		if (doJavadoc) {
			javadoc();
		}
	}

	@Override
	public void doPublish() {
		this.pack.signWithPgp =true;
		super.doPublish();
	}

}

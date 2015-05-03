package org.jerkar;

import org.jerkar.builtins.javabuild.JkJavaBuild;
import org.jerkar.publishing.JkMavenPublication;
import org.jerkar.publishing.JkMavenPublicationInfo;

public abstract class JerkarBuild extends JkJavaBuild {

	@Override
	public String sourceJavaVersion() {
		return JkJavaCompiler.V6;
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
		this.javadocMaker().process();
	}

}

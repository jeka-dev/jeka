package org.jerkar.scriptsamples;

import static org.jerkar.builtins.javabuild.JkPopularModules.*;

import org.jerkar.builtins.javabuild.JkJavaBuild;
import org.jerkar.depmanagement.JkDependencies;
import org.jerkar.depmanagement.JkVersion;
import org.jerkar.publishing.JkMavenPublication;
import org.jerkar.publishing.JkMavenPublicationInfo;

/**
 * This build demonstrate how to specified project metadata required to publish on 
 * Maven central ( see https://maven.apache.org/guides/mini/guide-central-repository-upload.html )
 * 
 * @author Jerome Angibaud
 */
public class OpenSourceJarBuild extends JkJavaBuild {
	
	@Override 
	protected JkVersion defaultVersion() {
		return JkVersion.ofName("1.3.1-SNAPSHOT");   
	}
	
	@Override
	protected JkDependencies dependencies() {
		return JkDependencies.builder()
			.on(GUAVA, "18.0") 
			.on(JUNIT, "4.11").scope(TEST)
		.build();
	}
	
	@Override  
	protected JkMavenPublication mavenPublication() {
		return super.mavenPublication().with(
			JkMavenPublicationInfo   // Information required to publish on Maven central
				.of("my project", "my description", "https://github.com/jerkar/samples")
				.withScm("https://github.com/jerkar/sample.git")
				.andApache2License()
				.andGitHubDeveloper("djeang", "dgeangdev@yahoo.fr")
			);
	}
	
}

package org.jerkar.scriptsamples;

import static org.jerkar.builtins.javabuild.JkPopularModules.GUAVA;
import static org.jerkar.builtins.javabuild.JkPopularModules.JAVAX_SERVLET_API;
import static org.jerkar.builtins.javabuild.JkPopularModules.JERSEY_SERVER;
import static org.jerkar.builtins.javabuild.JkPopularModules.JUNIT;
import static org.jerkar.builtins.javabuild.JkPopularModules.MOCKITO_ALL;

import org.jerkar.builtins.javabuild.JkJavaBuild;
import org.jerkar.depmanagement.JkDependencies;
import org.jerkar.depmanagement.JkVersion;

/**
 * Build sample for a jar project depending on several external modules.
 * This build :
 *     - produces jar, fat-jar, sources-jar by launching 'jerkar'
 *     - produces all this + javadoc by launching 'jerkar doDefault doc'
 *     - produces all this + publish on remote repository by typing 'jerkar doDefault doc publish'
 *
 * @author Jerome Angibaud
 */
public class BuildSampleClassic extends JkJavaBuild {
	
	@Override  // Optional : needless if you respect naming convention
	public String projectName() {
		return "script-samples";
	}
	
	@Override  // Optional : needless if you respect naming convention
	public String groupName() {
		return "org.jerkar";
	}
	
	@Override  // Optional : needless if you get the version from your SCM or version.txt resource
	protected JkVersion defaultVersion() {
		return JkVersion.named("0.3-SNAPSHOT");
	}
	
	@Override  // Optional :  needless if you use only local dependencies
	protected JkDependencies dependencies() {
		return JkDependencies.builder()
			.on(GUAVA, "18.0")   // Popular modules are available as Java constant
			.on(JERSEY_SERVER, "1.19")
			.on("com.orientechnologies:orientdb-client:2.0.8")
			.on(JAVAX_SERVLET_API, "2.5").scope(PROVIDED)
			.on(JUNIT, "4.11").scope(TEST)
			.on(MOCKITO_ALL, "1.9.5").scope(TEST)
		.build();
	}
	
	// Optional : usefull if you want quick run/debug your script in you IDE
	public static void main(String[] args) {
		BuildSampleClassic build = new BuildSampleClassic();
		build.doDefault();
		build.doc();
		build.publish();
	}
	
	
}

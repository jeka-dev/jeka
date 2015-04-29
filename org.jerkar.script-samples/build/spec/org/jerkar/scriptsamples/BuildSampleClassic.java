package org.jerkar.scriptsamples;

import static org.jerkar.builtins.javabuild.JkPopularModules.GUAVA;
import static org.jerkar.builtins.javabuild.JkPopularModules.JAVAX_SERVLET_API;
import static org.jerkar.builtins.javabuild.JkPopularModules.JERSEY_SERVER;
import static org.jerkar.builtins.javabuild.JkPopularModules.JUNIT;
import static org.jerkar.builtins.javabuild.JkPopularModules.MOCKITO_ALL;

import org.jerkar.builtins.javabuild.JkJavaBuild;
import org.jerkar.depmanagement.JkDependencies;

/**
 * This build is equivalent to {@link BuildSampleClassicExplicit} but removing 
 * the needless part cause we respect the convention project folder name = groupName.projectName
 * and the version number is taken from resource 'version.txt' (default behavior)
 *
 * @author Jerome Angibaud
 */
public class BuildSampleClassic extends JkJavaBuild {
	
	@Override  // Optional :  needless if you use only local dependencies
	protected JkDependencies dependencies() {
		return JkDependencies.builder()
			.on(GUAVA, "18.0")  
			.on(JERSEY_SERVER, "1.19")
			.on("com.orientechnologies:orientdb-client:2.0.8")
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

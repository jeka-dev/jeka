package org.jerkar.samples;

import static org.jerkar.api.depmanagement.JkPopularModules.JERSEY_SERVER;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkScope;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;

/**
 * This build is equivalent to {@link MavenStyleBuild} but removing 
 * the needless part cause we respect the convention project folder name = groupName.projectName
 * and the version number is taken from resource 'version.txt' (default behavior)
 *
 * @author Jerome Angibaud
 */
public class SimpleScopeBuild extends JkJavaBuild {
	
	private static final JkScope FOO = JkScope.of("foo"); 
	
	private static final JkScope BAR = JkScope.of("bar"); 
	
	@Override  // Optional :  needless if you use only local dependencies
	protected JkDependencies dependencies() {
		return JkDependencies.builder()
			.on(file("libs/foo.jar")).scope(COMPILE)  
			.on(JERSEY_SERVER, "1.19")
				.mapScope(COMPILE).to(RUNTIME)
				.and(FOO, PROVIDED).to(BAR, PROVIDED)
		.build();
	}
	
	
	
}

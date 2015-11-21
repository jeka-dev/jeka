package org.jerkar.samples;

import static org.jerkar.api.depmanagement.JkPopularModules.GUAVA;
import static org.jerkar.api.depmanagement.JkPopularModules.JERSEY_SERVER;
import static org.jerkar.api.depmanagement.JkPopularModules.JUNIT;
import static org.jerkar.api.depmanagement.JkPopularModules.MOCKITO_ALL;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.tool.JkInit;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;

/**
 * This build is equivalent to {@link MavenStyleBuild} but removing the needless
 * part cause we respect the convention project folder name =
 * groupName.projectName and the version number is taken from build.properties
 * (default behavior)
 *
 * @author Jerome Angibaud
 * @formatter:off
 */
public class AClassicBuild extends JkJavaBuild {

    {
	pack.checksums = "sha1";
    }

    @Override // Optional : needless if you use only local dependencies
    protected JkDependencies dependencies() {
	return JkDependencies.builder()
		.on(GUAVA, "18.0").scope(COMPILE)
		.on(JERSEY_SERVER, "1.19")
		.on("com.orientechnologies:orientdb-client:2.0.8")
		.on(JUNIT, "4.11").scope(TEST)
		.on(MOCKITO_ALL, "1.9.5").scope(TEST).build();
    }
    
    public static void main(String[] args) {
	JkInit.instanceOf(AClassicBuild.class, args).doDefault();
    }

}

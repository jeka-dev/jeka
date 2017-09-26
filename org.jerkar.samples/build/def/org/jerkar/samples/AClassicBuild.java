package org.jerkar.samples;

import static org.jerkar.api.depmanagement.JkJavaDepScopes.TEST;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.java.JkJavaVersion;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.tool.JkInit;
import org.jerkar.tool.JkJavaProjectBuild;



/**
 * This build is equivalent to {@link MavenStyleBuild} but removing the needless
 * part cause we respect the convention project folder name =
 * groupName.projectName and the version number is taken from build.properties
 * (default behavior)
 *
 * @author Jerome Angibaud
 * @formatter:off
 */
public class AClassicBuild extends JkJavaProjectBuild {

    {
	pack.checksums = "sha1";
	pack.tests = true;
	pack.javadoc = true;
    }
    
    @Override
    protected JkJavaProject createProject(JkJavaProject project) {
        JkDependencies dependencies = JkDependencies.builder()
            .on("com.google.guava:guava:18.0")
            .on("com.sun.jersey:jersey-server:1.19")
            .on("com.orientechnologies:orientdb-client:2.0.8")
            .on("junit:junit:4.11", TEST)
            .on("org.mockito:mockito-all:1.9.5", TEST).build();
        return project
                .setSourceVersion(JkJavaVersion.V7)
                .setDependencies(dependencies)
                .addFatJarArtifactFile("fat");
    }
    
    public static void main(String[] args) {
	    JkInit.instanceOf(AClassicBuild.class, args).doDefault();
    }

}

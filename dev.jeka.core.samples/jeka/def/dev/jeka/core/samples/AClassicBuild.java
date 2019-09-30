package dev.jeka.core.samples;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.api.java.project.JkJavaProjectMaker;
import dev.jeka.core.tool.JkCommands;
import dev.jeka.core.tool.JkImport;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.builtins.java.JkPluginJava;

import static dev.jeka.core.api.depmanagement.JkJavaDepScopes.TEST;


/**
 * This build is equivalent to {@link MavenStyleBuild} but removing the needless
 * part cause we respect the convention project folder name =
 * groupName.projectName and the version number is taken from build.properties
 * (default behavior)
 *
 * @author Jerome Angibaud
 * @formatter:off
 */
@JkImport("org.eclipse.jdt.core.compiler:ecj:4.6.1")
public class AClassicBuild extends JkCommands {

    public final JkPluginJava javaPlugin = getPlugin(JkPluginJava.class);

    AClassicBuild() {
	    javaPlugin.pack.checksums = "sha1";
	    javaPlugin.pack.tests = true;
	    javaPlugin.pack.javadoc = true;
    }
    
    @Override
    protected void setup() {
        JkJavaProject project = javaPlugin.getProject();
        project.setSourceVersion(JkJavaVersion.V8)
                .addDependencies(JkDependencySet.of()
                        .and("com.google.guava:guava:21.0")
                        .and("com.sun.jersey:jersey-server:1.19.4")
                        .and("junit:junit:4.11", TEST));
        JkJavaProjectMaker maker = project.getMaker();

         // With JDK 9 you should provide jre classes in the buildPath (so in dependencies)
        // maker.getTasksForCompilation().setCompiler(JkJavaCompiler.of(new EclipseCompiler()));
        maker.defineMainArtifactAsFatJar(true);  // project will produce a fat jar as well.
        maker.getTasksForTesting().setForkRun(true);
    }
    
    public static void main(String[] args) {
	    JkInit.instanceOf(AClassicBuild.class, args).javaPlugin.clean().pack();
    }

}

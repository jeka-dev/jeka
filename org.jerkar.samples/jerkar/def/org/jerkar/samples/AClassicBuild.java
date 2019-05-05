package org.jerkar.samples;

import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;
import org.jerkar.api.depmanagement.JkDependencySet;
import org.jerkar.api.java.JkJavaCompiler;
import org.jerkar.api.java.JkJavaVersion;
import org.jerkar.api.java.project.JkJavaProject;
import org.jerkar.api.java.project.JkJavaProjectMaker;
import org.jerkar.tool.JkImport;
import org.jerkar.tool.JkInit;
import org.jerkar.tool.JkRun;
import org.jerkar.tool.builtins.java.JkPluginJava;

import static org.jerkar.api.depmanagement.JkJavaDepScopes.TEST;

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
public class AClassicBuild extends JkRun {

    public final JkPluginJava javaPlugin = getPlugin(JkPluginJava.class);

    protected void AClassicBuild() {
	    javaPlugin.pack.checksums = "sha1";
	    javaPlugin.pack.tests = true;
	    javaPlugin.pack.javadoc = true;
    }
    
    @Override
    protected void setup() {
        JkJavaProject project = javaPlugin.getProject();
        project.setSourceVersion(JkJavaVersion.V7)
                .setDependencies(JkDependencySet.of()
                        .and("com.google.guava:guava:21.0")
                        .and("com.sun.jersey:jersey-server:1.19")
                        .and("junit:junit:4.11", TEST));
        JkJavaProjectMaker maker = project.getMaker();
        maker.getCompileTasks().setCompiler(JkJavaCompiler.of(new EclipseCompiler()));
        maker.defineMainArtifactAsFatJar(false);  // project will produce a fat jar as well.
        maker.getTestTasks().setForkRun(true);
    }
    
    public static void main(String[] args) {
	    JkInit.instanceOf(AClassicBuild.class, args).javaPlugin.clean().pack();
    }

}

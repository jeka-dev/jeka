package dev.jeka.core.samples;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.tool.JkCommandSet;
import dev.jeka.core.tool.JkCompileOption;
import dev.jeka.core.tool.JkDefClasspath;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.builtins.java.JkPluginJava;

import static dev.jeka.core.api.depmanagement.JkJavaDepScopes.TEST;


/**
 * This build is equivalent to {@link AClassicBuild} but removing the needless
 * part cause we respect the convention project folder name =
 * groupName.projectName and the version number is taken from build.properties
 * (default behavior)
 *
 * @author Jerome Angibaud
 * @formatter:off
 */
@JkDefClasspath("org.eclipse.jdt.core.compiler:ecj:4.6.1")
@JkCompileOption("-deprecation")
public class AClassicBuild extends JkCommandSet {

    public final JkPluginJava javaPlugin = getPlugin(JkPluginJava.class);

    AClassicBuild() {
	    javaPlugin.pack.checksums = "sha1";
	    javaPlugin.pack.tests = true;
	    javaPlugin.pack.javadoc = true;
    }
    
    @Override
    protected void setup() {
        JkJavaProject project = javaPlugin.getProject();
        project
            .getDependencyManagement()
                .addDependencies(JkDependencySet.of()
                        .and("com.google.guava:guava:21.0")
                        .and("com.sun.jersey:jersey-server:1.19.4")
                        .and("junit:junit:4.13", TEST)).__
            .getMaker()
                .defineMainArtifactAsFatJar(true)   // project will produce a fat jar as well.
                .getSteps()
                    .getTesting()
                        .getTestProcessor()
                            .setForkingProcess(true);
    }
    
    public static void main(String[] args) {
	    JkInit.instanceOf(AClassicBuild.class, args).javaPlugin.clean().pack();
    }


}

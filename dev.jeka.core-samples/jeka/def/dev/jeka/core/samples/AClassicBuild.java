package dev.jeka.core.samples;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkJavaDepScopes;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.JkClasspath;
import dev.jeka.core.api.java.JkManifest;
import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.api.java.project.JkJavaProjectMaker;
import dev.jeka.core.tool.JkCommandSet;
import dev.jeka.core.tool.JkCompileOption;
import dev.jeka.core.tool.JkDefClasspath;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.builtins.java.JkPluginJava;

import java.nio.file.Path;
import java.util.jar.Manifest;

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
        project.addDependencies(JkDependencySet.of()
                        .and("com.google.guava:guava:21.0")
                        .and("com.sun.jersey:jersey-server:1.19.4")
                        .and("junit:junit:4.13", TEST));
        JkJavaProjectMaker maker = project.getMaker();

        Path distribDir = project.getMaker().getOutLayout().getOutputPath().resolve("distrib");

        maker.defineMainArtifactAsFatJar(true);  // project will produce a fat jar as well.
        maker.getSteps().getPackaging()
                .
                .getManifest()
                    .merge(makeManifest(project, distribDir))
        maker.getSteps().getTesting().getTestProcessor().setForkingProcess(true);
    }
    
    public static void main(String[] args) {
	    JkInit.instanceOf(AClassicBuild.class, args).javaPlugin.clean().pack();
    }

    private static Manifest makeManifest(JkJavaProject project, Path distribDir) {
        JkPathSequence pathSequence = project.getMaker().fetchDependenciesFor(JkJavaDepScopes.RUNTIME);
        pathSequence.forEach(path -> JkPathFile.of(path).copyToDir(distribDir.resolve("libs")));

        // create Class-Path value
        StringBuilder cp = new StringBuilder();
        JkPathTree.of(distribDir).andMatching("libs/*.jar").getFiles().forEach(path -> cp.append(path + " "));

        // Create a Manifest and add it to the class dir at the proper location
        return JkManifest.of().addMainAttribute("class-path", cp.toString()).getManifest();
    }

}

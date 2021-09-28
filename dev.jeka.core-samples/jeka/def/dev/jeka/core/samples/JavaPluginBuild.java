package dev.jeka.core.samples;

import dev.jeka.core.api.depmanagement.JkPopularModules;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkTransitivity;
import dev.jeka.core.api.depmanagement.JkVersionedModule;
import dev.jeka.core.api.depmanagement.resolution.JkResolutionParameters;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.api.java.testing.JkTestProcessor;
import dev.jeka.core.api.tooling.intellij.JkImlGenerator;
import dev.jeka.core.tool.JkClass;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.builtins.java.JkPluginJava;
import dev.jeka.core.tool.builtins.maven.JkPluginPom;


/**
 * This builds a Java library and publish it on a maven repo using Java plugin. A Java library means a jar that
 * is not meant to be consumed by end-user but as a dependency of other Java projects.<p>
 *
 * @author Jerome Angibaud
 * @formatter:off
 */
public class JavaPluginBuild extends JkClass {

    public final JkPluginJava java = getPlugin(JkPluginJava.class);

    static final String JUNIT5_API = "org.junit.jupiter:junit-jupiter-api:5.8.1";
    
    @Override
    protected void setup() {
       java.getProject().simpleFacade()
               .setCompileDependencies(deps -> deps
                       .and("com.google.guava:guava:21.0")
                       .and("com.sun.jersey:jersey-server:1.19.4"))
               .setRuntimeDependencies(deps -> deps
                       .minus("org.junit.jupiter:junit-jupiter-engine")
                       .and("com.github.djeang:vincer-dom:1.2.0"))
               .setTestDependencies(deps -> deps
                       .and(JUNIT5_API))
               .addTestExcludeFilterSuffixedBy("IT", false)
               .setJavaVersion(JkJavaVersion.V8)
               .setPublishedMavenModuleId("dev.jeka:sample-javaplugin")
               .setPublishedMavenVersion("1.0-SNAPSHOT")
       .getProject()
           .getConstruction()
               .getDependencyResolver()
                    .getParams()
                            .setConflictResolver(JkResolutionParameters.JkConflictResolver.STRICT).__.__
               .getTesting()
                   .getTestProcessor()
                        .setForkingProcess(false)
                   .getEngineBehavior()
                        .setProgressDisplayer(JkTestProcessor.JkProgressOutputStyle.TREE).__.__.__.__
           .getPublication()
               .getMaven()
                    .addRepos(JkRepo.of(getOutputDir().resolve("test-output/maven-repo")))  // Use a dummy repo for demo purpose

                   // Published dependencies can be modified here from the ones declared in dependency management.
                   // Here jersey-server is not supposed to be part of the API but only needed at runtime.
                   .setDependencies(deps -> deps
                       .withTransitivity("com.sun.jersey:jersey-server", JkTransitivity.RUNTIME));
    }

    public void cleanPackPublish() {
        clean(); java.pack(); java.publish();
    }

    // For debugging purpose
    public void printIml() {
        JkImlGenerator imlGenerator = JkImlGenerator.of(this.java.getJavaIdeSupport());
        String iml = imlGenerator.generate();
        System.out.println(iml);
    }

    public void printMvn() {
        JkPluginPom pluginPom = getPlugin(JkPluginPom.class);
        pluginPom.dependencyCode();
    }

    public void showDependencies() {
        java.showDependenciesXml();
    }
    
    public static void main(String[] args) {
	    JkInit.instanceOf(JavaPluginBuild.class, args).cleanPackPublish();
    }


}

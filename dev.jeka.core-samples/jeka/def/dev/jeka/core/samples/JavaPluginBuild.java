package dev.jeka.core.samples;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.java.testing.JkTestProcessor;
import dev.jeka.core.api.java.testing.JkTestSelection;
import dev.jeka.core.tool.JkCommandSet;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.builtins.java.JkPluginJava;

import static dev.jeka.core.api.depmanagement.JkScope.RUNTIME;
import static dev.jeka.core.api.depmanagement.JkScope.TEST;


/**
 * This builds a Java library and publish it on a maven repo using Java plugin. A Java library means a jar that
 * is not meant to be consumed by end-user but as a dependency of other Java projects.<p>
 *
 * @author Jerome Angibaud
 * @formatter:off
 */
public class JavaPluginBuild extends JkCommandSet {

    public final JkPluginJava java = getPlugin(JkPluginJava.class);
    
    @Override
    protected void setup() {
       java.getProject()
           .getDependencyManagement()
               .addDependencies(JkDependencySet.of()
                   .and("com.google.guava:guava:21.0")
                   .and("com.sun.jersey:jersey-server:1.19.4")
                   .and("org.junit.jupiter:junit-jupiter-engine:5.1.0", TEST)
                   .and("org.junit.vintage:junit-vintage-engine:jar:5.6.0", TEST)).__
           .getProduction()
               .getCompilation()
                   .setJavaVersion(JkJavaVersion.V8).__.__
           .getTesting()
               .getTestSelection()
                    .addIncludeStandardPatterns()
                    .addIncludePatterns(JkTestSelection.IT_INCLUDE_PATTERN).__
               .getTestProcessor()
                    .setForkingProcess(false)
               .getEngineBehavior()
                    .setProgressDisplayer(JkTestProcessor.JkProgressOutputStyle.TREE).__.__.__

           // Publication is only necessary if your project is being deployed on a binary repository.
           // Many projects as jee war jar, springboot application, tools, Graphical application
           // does not need this section at all.
           .getPublication()
               .setModuleId("dev.jeka:sample-javaplugin")
               //.setVersion(JkGitWrapper.of(getBaseDir()).getVersionFromTags())  // Version inferred from Git
               .setVersion("1.0-SNAPSHOT")
               .addRepos(JkRepo.ofMaven(getOutputDir().resolve("test-output/maven-repo")))  // Use a dummy repo for demo purpose
               .getMavenPublication()

                   // Published dependencies can be modified here from the ones declared in dependency management.
                   // Here jersey-server is not supposed to be part of the API but only needed at runtime.
                   .setDependencies(deps -> deps
                       .replaceScope("com.sun.jersey:jersey-server", RUNTIME));
    }

    public void cleanPackPublish() {
        clean(); java.pack(); java.publish();
    }
    
    public static void main(String[] args) {
	    JkInit.instanceOf(JavaPluginBuild.class, args).cleanPackPublish();
    }


}

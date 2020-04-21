package dev.jeka.core.samples;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.tooling.JkGitWrapper;
import dev.jeka.core.tool.JkCommandSet;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.builtins.java.JkPluginJava;

import static dev.jeka.core.api.depmanagement.JkJavaDepScopes.RUNTIME;
import static dev.jeka.core.api.depmanagement.JkJavaDepScopes.TEST;


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
                    .and("junit:junit:4.13", TEST)).__
            .getPublication()
               .setModuleId("dev.jeka:sample-javaplugin")
               .setVersion(JkGitWrapper.of(getBaseDir()).getVersionFromTags())  // Version inferred from Git

               // Published dependencies can be modified here from the ones declared in dependency management.
               // Here jersey-server is not supposed to be part of the API but only needed at runtime.
               .setDependencies(deps -> deps
                   .replaceScope("com.sun.jersey:jersey-server", RUNTIME))

               // Use a dummy repo for demo purpose
               .addRepos(JkRepo.ofMaven(getOutputDir().resolve("test-output/maven-repo")));
    }

    public void cleanPackPublish() {
        clean(); java.pack(); java.publish();
    }
    
    public static void main(String[] args) {
	    JkInit.instanceOf(JavaPluginBuild.class, args).cleanPackPublish();
    }


}

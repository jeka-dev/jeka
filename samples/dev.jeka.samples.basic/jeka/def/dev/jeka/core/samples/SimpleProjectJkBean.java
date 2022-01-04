package dev.jeka.core.samples;

import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkTransitivity;
import dev.jeka.core.api.depmanagement.resolution.JkResolutionParameters;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.java.testing.JkTestProcessor;
import dev.jeka.core.api.tooling.intellij.JkImlGenerator;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.builtins.maven.PomJkBean;
import dev.jeka.core.tool.builtins.project.ProjectJkBean;


/**
 * This builds a Java library and publish it on a maven repo using Project plugin. A Java library means a jar that
 * is not meant to be consumed by end-user but as a dependency of other Java projects.<p>
 *
 * @author Jerome Angibaud
 * @formatter:off
 */
public class SimpleProjectJkBean extends JkBean {

    public final ProjectJkBean projectPlugin = getRuntime().getBean(ProjectJkBean.class);

    static final String JUNIT5 = "org.junit.jupiter:junit-jupiter:5.8.1";
    
    @Override
    protected void init() {
       projectPlugin.getProject().simpleFacade()
               .configureCompileDeps(deps -> deps
                   .and("com.google.guava:guava:30.0-jre")
                   .and("com.sun.jersey:jersey-server:1.19.4")
               )
               .configureRuntimeDeps(deps -> deps
                   .and("com.github.djeang:vincer-dom:1.2.0")
               )
               .configureTestDeps(deps -> deps
                   .and(JUNIT5)
               )
               .addTestExcludeFilterSuffixedBy("IT", false)
               .setJvmTargetVersion(JkJavaVersion.V8)
               .setPublishedModuleId("dev.jeka:sample-javaplugin")
               .setPublishedVersion("1.0-SNAPSHOT")
       .getProject()
           .getConstruction()
               .getCompiler()
                    .setForkedWithDefaultProcess()
               .__
               .getDependencyResolver()
                    .getDefaultParams()
                        .setConflictResolver(JkResolutionParameters.JkConflictResolver.STRICT)
                    .__
               .__
               .getTesting()
                    .getTestProcessor()
                        .setForkingProcess(false)
                        .getEngineBehavior()
                            .setProgressDisplayer(JkTestProcessor.JkProgressOutputStyle.TREE)
                        .__
                    .__
               .__
           .__
           .getPublication()
               .getMaven()
                    .addRepos(JkRepo.of(getOutputDir().resolve("test-output/maven-repo")))  // Use a dummy repo for demo purpose

                   // Published dependencies can be modified here from the ones declared in dependency management.
                   // Here jersey-server is not supposed to be part of the API but only needed at runtime.
                   .configureDependencies(deps -> deps
                       .withTransitivity("com.sun.jersey:jersey-server", JkTransitivity.RUNTIME));
    }

    public void cleanPackPublish() {
        clean(); projectPlugin.pack(); projectPlugin.publishLocal();
    }

    // For debugging purpose
    public void printIml() {
        JkImlGenerator imlGenerator = JkImlGenerator.of().setIdeSupport(this.projectPlugin.getJavaIdeSupport());
        String iml = imlGenerator.computeIml().toDoc().toXml();
        System.out.println(iml);
    }

    public void printMvn() {
        PomJkBean pluginPom = getRuntime().getBean(PomJkBean.class);
        pluginPom.dependencyCode();
    }

    public void showDependencies() {
        projectPlugin.showDependenciesXml();
    }
    
    public static void main(String[] args) {
	    JkInit.instanceOf(SimpleProjectJkBean.class, args).cleanPackPublish();
    }


}

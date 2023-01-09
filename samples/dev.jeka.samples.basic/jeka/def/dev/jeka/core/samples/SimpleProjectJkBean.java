package dev.jeka.core.samples;

import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkTransitivity;
import dev.jeka.core.api.depmanagement.resolution.JkResolutionParameters;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.testing.JkTestProcessor;
import dev.jeka.core.api.tooling.intellij.JkImlGenerator;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.builtins.maven.MavenJkBean;
import dev.jeka.core.tool.builtins.project.ProjectJkBean;


/**
 * This builds a Java library and publish it on a maven repo using Project plugin. A Java library means a jar that
 * is not meant to be consumed by end-user but as a dependency of other Java projects.<p>
 *
 * @author Jerome Angibaud
 * @formatter:off
 */
public class SimpleProjectJkBean extends JkBean {

    public final ProjectJkBean projectPlugin = getBean(ProjectJkBean.class).configure(this::configure);

    static final String JUNIT5 = "org.junit.jupiter:junit-jupiter:5.8.1";

    public String checkedValue;

    private void configure(JkProject project) {
       project.flatFacade()
               .configureCompileDependencies(deps -> deps
                   .and("com.google.guava:guava:30.0-jre")
                   .and("com.sun.jersey:jersey-server:1.19.4")
               )
               .configureTestDependencies(deps -> deps
                   .and(JUNIT5)
               )
               .addTestExcludeFilterSuffixedBy("IT", false);

       project
           .setJvmTargetVersion(JkJavaVersion.V8)
           .compiler
                .setForkedWithDefaultProcess();
       project
           .dependencyResolver
                .getDefaultParams()
                    .setConflictResolver(JkResolutionParameters.JkConflictResolver.STRICT);
       project
           .packaging
               .configureRuntimeDependencies(deps -> deps
                       .and("com.github.djeang:vincer-dom:1.2.0")
               );
       project
           .testing
                .testProcessor
                    .setForkingProcess(false)
                    .engineBehavior
                        .setProgressDisplayer(JkTestProcessor.JkProgressOutputStyle.TREE);
       project
           .publication
               .setModuleId("dev.jeka:sample-javaplugin")
               .setVersion("1.0-SNAPSHOT")
               .maven
                    .addRepos(JkRepo.of(getOutputDir().resolve("test-output/maven-repo")))  // Use a dummy repo for demo purpose

               // Published dependencies can be modified here from the ones declared in dependency management.
               // Here jersey-server is not supposed to be part of the API but only needed at runtime.
               .configureDependencies(deps -> deps
                   .withTransitivity("com.sun.jersey:jersey-server", JkTransitivity.RUNTIME));
    }

    public void cleanPackPublish() {
        cleanOutput(); projectPlugin.pack(); projectPlugin.publishLocal();
    }

    public void checkValueIsA() {
        JkUtilsAssert.state("A".equals(checkedValue), "checkedValue field values %s and not 'A'.", checkedValue);
        JkUtilsAssert.state("foo".equals(getRuntime().getProperties().get("my.prop")),"Project property 'my.prop' not found.");
    }

    // For debugging purpose
    public void printIml() {
        JkImlGenerator imlGenerator = JkImlGenerator.of().setIdeSupport(this.projectPlugin.getJavaIdeSupport());
        String iml = imlGenerator.computeIml().toDoc().toXml();
        System.out.println(iml);
    }

    public void printMvn() {
        MavenJkBean pluginPom = getRuntime().getBean(MavenJkBean.class);
        pluginPom.migrateToCode();
    }

    public void showDependencies() {
        projectPlugin.showDependenciesXml();
    }
    
    public static void main(String[] args) {
	    SimpleProjectJkBean bean = JkInit.instanceOf(SimpleProjectJkBean.class, args, "checkedValue=A");
        bean.cleanPackPublish();
        bean.checkValueIsA();
    }


}

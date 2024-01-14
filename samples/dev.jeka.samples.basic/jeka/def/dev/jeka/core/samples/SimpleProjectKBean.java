package dev.jeka.core.samples;

import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkTransitivity;
import dev.jeka.core.api.depmanagement.resolution.JkResolutionParameters;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.testing.JkTestProcessor;
import dev.jeka.core.api.tooling.intellij.JkImlGenerator;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.tooling.maven.MavenMigrationKBean;


/**
 * This builds a Java library and publish it on a maven repo using Project plugin. A Java library means a jar that
 * is not meant to be consumed by end-user but as a dependency of other Java projects.<p>
 *
 * @author Jerome Angibaud
 */
public class SimpleProjectKBean extends KBean {

    public final ProjectKBean projectKBean = load(ProjectKBean.class);

    @JkDoc("If true, skip execution of Integration tests.")
    public boolean skipIT;

    static final String JUNIT5 = "org.junit.jupiter:junit-jupiter:5.8.1";

    public String checkedValue;

    @Override
    protected void init() {
        JkProject project = projectKBean.project;
        project.flatFacade()
               .addCompileDeps(
                       "com.google.guava:guava:30.0-jre",
                       "com.sun.jersey:jersey-server:1.19.4"
               )
               .addTestDeps(
                       "org.junit.jupiter:junit-jupiter:5.10.1"
               )
               .addTestExcludeFilterSuffixedBy("IT", skipIT);
       project
           .setJvmTargetVersion(JkJavaVersion.V8)
           .compilerToolChain
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
       project.mavenPublication
               .setModuleId("dev.jeka:sample-javaplugin")
               .setVersion("1.0-SNAPSHOT")
               .addRepos(JkRepo.of(getOutputDir().resolve("test-output/maven-repo")))  // Use a dummy repo for demo purpose

               // Published dependencies can be modified here from the ones declared in dependency management.
               // Here jersey-server is not supposed to be part of the API but only needed at runtime.
               .configureDependencies(deps -> deps
                   .withTransitivity("com.sun.jersey:jersey-server", JkTransitivity.RUNTIME));
    }

    public void cleanPackPublish() {
        cleanOutput(); projectKBean.pack(); projectKBean.publishLocal();
    }

    public void checkValueIsA() {
        JkUtilsAssert.state("A".equals(checkedValue), "checkedValue field values %s and not 'A'.", checkedValue);
        JkUtilsAssert.state("foo".equals(getRuntime().getProperties().get("my.prop")),"Project property 'my.prop' not found.");
    }

    // For debugging purpose
    public void printIml() {
        JkImlGenerator imlGenerator = JkImlGenerator.of().setIdeSupport(projectKBean::getJavaIdeSupport);
        String iml = imlGenerator.computeIml().toDoc().toXml();
        System.out.println(iml);
    }

    public void printMvn() {
        MavenMigrationKBean pluginPom = getRuntime().load(MavenMigrationKBean.class);
        pluginPom.showDepsAsCode();
    }

    public void showDependencies() {
        projectKBean.showDepTreesAsXml();
    }
    
    public static void main(String[] args) {
	    SimpleProjectKBean bean = JkInit.instanceOf(SimpleProjectKBean.class, args, "checkedValue=A");
        bean.cleanPackPublish();
        bean.checkValueIsA();
    }


}

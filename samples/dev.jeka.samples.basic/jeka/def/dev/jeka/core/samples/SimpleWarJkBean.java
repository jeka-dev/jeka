package dev.jeka.core.samples;

import dev.jeka.core.api.depmanagement.JkRepoProperties;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactProducer;
import dev.jeka.core.api.j2e.JkJ2eWarProjectAdapter;
import dev.jeka.core.api.java.JkJavaProcess;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.builtins.project.ProjectJkBean;

import java.nio.file.Path;

/**
 * This builds a Java library and publish it on a maven repo using Project plugin. A Java library means a jar that
 * is not meant to be consumed by end-user but as a dependency of other Java projects.<p>
 *
 * @author Jerome Angibaud
 * @formatter:off
 */
public class SimpleWarJkBean extends JkBean {

    public String port = "8080";

    public String jettyRunnerVersion = "9.4.28.v20200408";

    ProjectJkBean projectPlugin = getBean(ProjectJkBean.class).configure(this::configure);

    private void configure(JkProject project) {
       project.simpleFacade()
               .configureCompileDeps(deps -> deps
                       .and("com.google.guava:guava:30.0-jre")
                       .and("javax.servlet:javax.servlet-api:4.0.1"))
               .setPublishedModuleId("dev.jeka.samples:war-project")
               .setPublishedVersion("1.0-SNAPSHOT")
               .configureRuntimeDeps(compileDeps -> compileDeps
                       .minus("javax.servlet:javax.servlet-api"))
               .setJvmTargetVersion(JkJavaVersion.V8)
               .includeJavadocAndSources(false, false)
               .getProject()
                        .getCompilation()
                            .getLayout()
                                .emptySources().addSource("src/main/javaweb").__.__
               .getTesting()
                   .setSkipped(true);
       JkJ2eWarProjectAdapter.of()
               .configure(project);
    }

    public void cleanPackRun() {
        cleanOutput(); projectPlugin.pack(); projectPlugin.publishLocal();
    }

    public void check() {
        runWarWithJetty();
    }

    public void runWarWithJetty() {
        JkArtifactProducer artifactProducer = projectPlugin.getProject().getArtifactProducer();
        artifactProducer.makeMissingArtifacts();
        Path jettyRunner = JkRepoProperties.of(getRuntime().getProperties()).getDownloadRepos().get("org.eclipse.jetty:jetty-runner:"
                + jettyRunnerVersion);
        JkJavaProcess.ofJavaJar(jettyRunner, null)
                .exec(artifactProducer.getMainArtifactPath().toString(), "--port", port);
    }
    
    public static void main(String[] args) {
	    JkInit.instanceOf(SimpleWarJkBean.class, args, "-LS=DEBUG").cleanPackRun();
    }


}

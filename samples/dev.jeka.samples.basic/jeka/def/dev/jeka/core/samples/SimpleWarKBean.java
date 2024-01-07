package dev.jeka.core.samples;

import dev.jeka.core.api.depmanagement.JkRepoProperties;
import dev.jeka.core.api.j2e.JkJ2eWarProjectAdapter;
import dev.jeka.core.api.java.JkJavaProcess;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;

import java.nio.file.Path;

/**
 * This builds a Java library and publish it on a maven repo using Project plugin. A Java library means a jar that
 * is not meant to be consumed by end-user but as a dependency of other Java projects.<p>
 *
 * @author Jerome Angibaud
 * @formatter:off
 */
public class SimpleWarKBean extends KBean {

    ProjectKBean projectKBean = load(ProjectKBean.class);

    public String port = "8080";

    public String jettyRunnerVersion = "9.4.28.v20200408";

    @Override
    protected void init() {
        JkProject project = projectKBean.project;
        project.flatFacade()
                   .configureCompileDependencies(deps -> deps
                           .and("com.google.guava:guava:30.0-jre")
                           .and("javax.servlet:javax.servlet-api:4.0.1"))
                   .setModuleId("dev.jeka.samples:war-project")
                   .setVersion("1.0-SNAPSHOT")
                   .configureRuntimeDependencies(compileDeps -> compileDeps
                           .minus("javax.servlet:javax.servlet-api"))
                   .setJvmTargetVersion(JkJavaVersion.V8);

        project.compilation.layout.emptySources().addSource("src/main/javaweb");
        project.testing.setSkipped(true);
        JkJ2eWarProjectAdapter.of().configure(project);
    }

    public void cleanPackRun() {
        cleanOutput(); projectKBean.pack(); projectKBean.publishLocal();
    }

    public void check() {
        runWarWithJetty();
    }

    public void runWarWithJetty() {
        projectKBean.pack();
        Path jettyRunner = JkRepoProperties.of(getRuntime().getProperties()).getDownloadRepos().get("org.eclipse.jetty:jetty-runner:"
                + jettyRunnerVersion);
        JkJavaProcess.ofJavaJar(jettyRunner, null)
                .setParams(projectKBean.project.artifactLocator.getMainArtifactPath().toString(), "--port", port).exec();
    }
    
    public static void main(String[] args) {
	    JkInit.instanceOf(SimpleWarKBean.class, args, "-LS=DEBUG").cleanPackRun();
    }


}

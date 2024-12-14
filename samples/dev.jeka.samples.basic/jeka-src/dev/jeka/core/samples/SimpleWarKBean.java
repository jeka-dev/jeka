package dev.jeka.core.samples;

import dev.jeka.core.api.depmanagement.JkRepoProperties;
import dev.jeka.core.api.j2e.JkJ2eWarProjectAdapter;
import dev.jeka.core.api.java.JkJavaProcess;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.project.JkProjectPublications;
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

    final JkProject project = load(ProjectKBean.class).project;

    public String port = "8080";

    public String jettyRunnerVersion = "9.4.28.v20200408";

    @Override
    protected void init() {
        project.setModuleId("dev.jeka.samples:war-project")
                .setVersion("1.0-SNAPSHOT")
                .setJvmTargetVersion(JkJavaVersion.V8)
                .compilation.layout.emptySources().addSource("src/main/javaweb");
        project.testing.setSkipped(true);

        project.flatFacade.dependencies.compile.modify(deps -> deps
                .and("com.google.guava:guava:30.0-jre")
                .and("javax.servlet:javax.servlet-api:4.0.1"));
        project.flatFacade.dependencies.runtime
                .remove("javax.servlet:javax.servlet-api");
        JkJ2eWarProjectAdapter.of().configure(project);
    }

    public void cleanPackRun() {
        project.clean().pack();
        JkProjectPublications.mavenPublication(project).publishLocal();
    }

    public void check() {
        runWarWithJetty();
    }

    public void runWarWithJetty() {
        project.pack();
        Path jettyRunner = JkRepoProperties.of(getRunbase().getProperties()).getDownloadRepos().get("org.eclipse.jetty:jetty-runner:"
                + jettyRunnerVersion);
        JkJavaProcess.ofJavaJar(jettyRunner, null)
                .addParams(project.artifactLocator.getMainArtifactPath().toString(), "--port", port).exec();
    }
    
    public static void main(String[] args) {
	    JkInit.kbean(SimpleWarKBean.class, args, "-LS=DEBUG").cleanPackRun();
    }


}

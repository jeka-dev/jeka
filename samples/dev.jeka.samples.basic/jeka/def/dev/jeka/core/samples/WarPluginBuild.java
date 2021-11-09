package dev.jeka.core.samples;

import dev.jeka.core.api.depmanagement.artifact.JkArtifactProducer;
import dev.jeka.core.api.java.JkJavaProcess;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.tool.JkClass;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.JkRepoFromOptions;
import dev.jeka.core.tool.builtins.project.JkPluginProject;
import dev.jeka.core.tool.builtins.project.JkPluginWar;

import java.nio.file.Path;

/**
 * This builds a Java library and publish it on a maven repo using Project plugin. A Java library means a jar that
 * is not meant to be consumed by end-user but as a dependency of other Java projects.<p>
 *
 * @author Jerome Angibaud
 * @formatter:off
 */
public class WarPluginBuild extends JkClass {

    public int port = 8080;

    public String jettyRunnerVersion = "9.4.28.v20200408";

    JkPluginProject java = getPlugin(JkPluginProject.class);

    JkPluginWar war = getPlugin(JkPluginWar.class);

    @Override
    protected void setup() {
       java.getProject().simpleFacade()
               .setCompileDependencies(deps -> deps
                       .and("com.google.guava:guava:30.0-jre")
                       .and("javax.servlet:javax.servlet-api:4.0.1"))
               .setRuntimeDependencies(compileDeps -> compileDeps
                       .minus("javax.servlet:javax.servlet-api"))
               .setJvmTargetVersion(JkJavaVersion.V8)
               .getProject()
                    .getConstruction()
                        .getCompilation()
                            .getLayout()
                                .emptySources().addSource("src/main/javaweb").__.__
               .getTesting()
                   .setSkipped(true);
    }

    public void cleanPackRun() {
        clean(); java.pack(); runWarWithJetty();
    }

    public void runWarWithJetty() {
        JkArtifactProducer artifactProducer = java.getProject().getPublication().getArtifactProducer();
        artifactProducer.makeMissingArtifacts();
        Path jettyRunner = JkRepoFromOptions.getDownloadRepo().toSet().get("org.eclipse.jetty:jetty-runner:"
                + jettyRunnerVersion);
        JkJavaProcess.ofJavaJar(jettyRunner, null)
                .exec(artifactProducer.getMainArtifactPath().toString(), "--port", Integer.toString(port));
    }
    
    public static void main(String[] args) {
	    JkInit.instanceOf(WarPluginBuild.class, args).cleanPackRun();
    }


}

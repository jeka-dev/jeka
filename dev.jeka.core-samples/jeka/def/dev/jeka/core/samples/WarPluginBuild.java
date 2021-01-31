package dev.jeka.core.samples;

import dev.jeka.core.api.depmanagement.JkArtifactProducer;
import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkScope;
import dev.jeka.core.api.java.JkJavaProcess;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.tool.JkClass;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.builtins.java.JkPluginJava;
import dev.jeka.core.tool.builtins.java.JkPluginWar;
import dev.jeka.core.tool.builtins.repos.JkPluginRepo;

import java.nio.file.Path;

/**
 * This builds a Java library and publish it on a maven repo using Java plugin. A Java library means a jar that
 * is not meant to be consumed by end-user but as a dependency of other Java projects.<p>
 *
 * @author Jerome Angibaud
 * @formatter:off
 */
public class WarPluginBuild extends JkClass {

    public int port = 8080;

    public String jettyRunnerVersion = "9.4.28.v20200408";

    JkPluginJava java = getPlugin(JkPluginJava.class);

    JkPluginWar war = getPlugin(JkPluginWar.class);

    @Override
    protected void setup() {
       java.getProject().simpleFacade()
               .addDependencies(JkDependencySet.of()
                       .and("com.google.guava:guava:21.0")
                       .and("javax.servlet:javax.servlet-api:jar:4.0.1", JkScope.COMPILE))
               .setJavaVersion(JkJavaVersion.V8).getProject().getConstruction()
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
        JkPluginRepo repo = getPlugin(JkPluginRepo.class);
        Path jettyRunner = repo.downloadRepository().toSet().get("org.eclipse.jetty:jetty-runner:" + jettyRunnerVersion);
        JkJavaProcess.of()
                .runJarSync(jettyRunner, artifactProducer.getMainArtifactPath().toString(), "--port", Integer.toString(port));
    }
    
    public static void main(String[] args) {
	    JkInit.instanceOf(WarPluginBuild.class, args).cleanPackRun();
    }


}

import dev.jeka.core.api.depmanagement.publication.JkMavenPublication;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.maven.JkMavenPublications;

public class PureApi {

    public static void main(String[] args) {
        JkLog.setDecorator(JkLog.Style.INDENT);  // activate console logging

        // A project with ala Maven layout (src/main/javaPlugin, src/test/javaPlugin, ...)
        JkProject coreProject = JkProject.of().flatFacade()
                .setBaseDir("../dev.jeka.samples.basic")
                .customizeTestDeps(deps -> deps
                    .and("junit:junit:4.13")).getProject();

        // A project depending on the first project + Guava
        JkProject dependerProject = JkProject.of().flatFacade()
                .customizeCompileDeps(deps -> deps
                    .and("com.google.guava:guava:30.0-jre")
                    .and(coreProject.toDependency()))
                .setModuleId("mygroup:depender")
                .setVersion("1.0-SNAPSHOT").getProject();

        dependerProject.pack();
        JkMavenPublications.of(dependerProject).publish();
    }
}

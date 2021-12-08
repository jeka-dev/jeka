import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;

public class PureApi {

    public static void main(String[] args) {
        JkLog.setDecorator(JkLog.Style.INDENT);  // activate console logging

        // A project with ala Maven layout (src/main/javaPlugin, src/test/javaPlugin, ...)
        JkProject coreProject = JkProject.of().simpleFacade()
                .setBaseDir("../dev.jeka.samples.basic")
                .configureTestDeps(deps -> deps
                    .and("junit:junit:4.13")).getProject();

        // A project depending on the first project + Guava
        JkProject dependerProject = JkProject.of().simpleFacade()
                .configureCompileDeps(deps -> deps
                    .and("com.google.guava:guava:30.0-jre")
                    .and(coreProject.toDependency()))
                .setPublishedModuleId("mygroup:depender")
                .setPublishedVersion("1.0-SNAPSHOT").getProject();

        dependerProject.getArtifactProducer().makeAllArtifacts();
        dependerProject.getPublication().publish();
    }
}

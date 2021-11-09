import dev.jeka.core.api.project.JkJavaProject;
import dev.jeka.core.api.system.JkLog;

public class PureApi {

    public static void main(String[] args) {
        JkLog.setDecorator(JkLog.Style.INDENT);  // activate console logging

        // A project with ala Maven layout (src/main/javaPlugin, src/test/javaPlugin, ...)
        JkJavaProject coreProject = JkJavaProject.of().simpleFacade()
                .setBaseDir("../dev.jeka.samples.basic")
                .setTestDependencies(deps -> deps
                    .and("junit:junit:4.13")).getProject();

        // A project depending on the first project + Guava
        JkJavaProject dependerProject = JkJavaProject.of().simpleFacade()
                .setCompileDependencies(deps -> deps
                    .and("com.google.guava:guava:30.0-jre")
                    .and(coreProject.toDependency()))
                .setPublishedMavenModuleId("mygroup:depender")
                .setPublishedMavenVersion("1.0-SNAPSHOT").getProject();

        dependerProject.getPublication().getArtifactProducer().makeAllArtifacts();
        dependerProject.getPublication().publish();
    }
}

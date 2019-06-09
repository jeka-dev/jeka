import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkJavaDepScopes;
import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.api.system.JkLog;

public class PureApi {

    public static void main(String[] args) {
        JkLog.registerHierarchicalConsoleHandler();  // activate console logging

        // A project with ala Maven layout (src/main/javaPlugin, src/test/javaPlugin, ...)
        JkJavaProject coreProject = JkJavaProject.ofMavenLayout("../org.jerkar.samples");
        coreProject.addDependencies(
                JkDependencySet.of().and("junit:junit:4.11", JkJavaDepScopes.TEST));

        // A project depending on the first project + Guava
        JkJavaProject dependerProject = JkJavaProject.ofMavenLayout(".");
        dependerProject.setVersionedModule("mygroup:depender", "1.0-SNAPSHOT");
        dependerProject.addDependencies(JkDependencySet.of()
                .and("com.google.guava:guava:22.0")
                .and(coreProject));

        coreProject.getMaker().clean();
        dependerProject.getMaker().clean().makeAllArtifacts();
        dependerProject.getMaker().getTasksForPublishing().publish();
    }
}

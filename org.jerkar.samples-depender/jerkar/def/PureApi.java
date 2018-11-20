import org.jerkar.api.depmanagement.JkDependencySet;
import org.jerkar.api.depmanagement.JkJavaDepScopes;
import org.jerkar.api.java.project.JkJavaProject;
import org.jerkar.api.system.JkLog;

public class PureApi {

    public static void main(String[] args) {
        JkLog.registerHierarchicalConsoleHandler();  // activate console logging

        // A project with ala Maven layout (src/main/java, src/test/java, ...)
        JkJavaProject coreProject = JkJavaProject.ofMavenLayout("../org.jerkar.samples");
        coreProject.addDependencies(
                JkDependencySet.of().and("junit:junit:4.11", JkJavaDepScopes.TEST));

        // A project depending on the first project + Guava
        JkJavaProject dependerProject = JkJavaProject.ofMavenLayout(".");
        dependerProject.addDependencies(JkDependencySet.of()
                .and("com.google.guava:guava:22.0")
                .and(coreProject));

        coreProject.getMaker().clean();
        dependerProject.getMaker().clean().makeAllArtifacts();
        dependerProject.getMaker().getPublishTasks().publish();
    }
}

import org.jerkar.api.depmanagement.JkDependencySet;
import org.jerkar.api.depmanagement.JkJavaDepScopes;
import org.jerkar.api.java.project.JkJavaProject;
import org.jerkar.api.system.JkLog;

public class PureApi {

    public static void main(String[] args) {
        JkLog.registerHierarchicalConsoleHandler();

        JkJavaProject sampleProject = JkJavaProject.ofMavenLayout("../org.jerkar.samples");
        sampleProject.addDependencies(
                JkDependencySet.of().and("junit:junit:4.11", JkJavaDepScopes.TEST));

        JkJavaProject dependerProject = JkJavaProject.ofMavenLayout(".");
        dependerProject.addDependencies(JkDependencySet.of()
                .and("com.google.guava:guava:22.0")
                .and(sampleProject));

        sampleProject.getMaker().clean();
        dependerProject.getMaker().defineMainArtifactAsFatJar(false);
        dependerProject.getMaker().clean().makeAllArtifacts();
    }
}

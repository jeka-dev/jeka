import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkJavaDepScopes;
import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.api.system.JkLog;

import java.nio.file.Paths;

public class PureApi {

    public static void main(String[] args) {
        JkLog.setHierarchicalConsoleConsumer();  // activate console logging

        // A project with ala Maven layout (src/main/javaPlugin, src/test/javaPlugin, ...)
        JkJavaProject coreProject = JkJavaProject.of()
            .setBaseDir(Paths.get("../dev.jeka.core-samples"))
             .getDependencyManagement()
                .addDependencies(JkDependencySet.of()
                        .and("junit:junit:4.13", JkJavaDepScopes.TEST)).__;

        // A project depending on the first project + Guava
        JkJavaProject dependerProject = JkJavaProject.of()
            .getDependencyManagement()
                .addDependencies(JkDependencySet.of()
                    .and("com.google.guava:guava:22.0")
                    .and(coreProject.asDependency())).__
                .getPublication()
                    .setModuleId("mygroup:depender")
                    .setVersion(JkVersion.of("1.0-SNAPSHOT")).__;
        dependerProject.getArtifactProducer().makeAllArtifacts();
        dependerProject.getPublication().publish();
    }
}

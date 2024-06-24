import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.project.JkProjectPublications;
import dev.jeka.core.api.system.JkLog;

public class PureApi {

    public static void main(String[] args) {
        JkLog.setDecorator(JkLog.Style.INDENT);  // activate console logging

        // A project with ala Maven layout (src/main/javaPlugin, src/test/javaPlugin, ...)
        JkProject coreProject = JkProject.of().flatFacade
                .setBaseDir("../dev.jeka.samples.basic")
                .getProject();
        coreProject.testing.compilation.dependencies
                    .add("junit:junit:4.13");

        // A project depending on the first project + Guava
        JkProject dependerProject = JkProject.of().flatFacade
                .setModuleId("mygroup:depender")
                .setVersion("1.0-SNAPSHOT").getProject();
        dependerProject.compilation.dependencies
                .add("com.google.guava:guava:30.0-jre")
                .add(coreProject.toDependency());

        dependerProject.pack();
        JkProjectPublications.mavenPublication(dependerProject).publish();
    }
}

import dev.jeka.core.samples.JavaPluginBuild;
import dev.jeka.core.tool.JkClass;
import dev.jeka.core.tool.JkDefImport;
import dev.jeka.core.tool.builtins.project.JkPluginProject;

/**
 * Simple build demonstrating of how Jeka can handle multi-project build.
 * 
 * @author Jerome Angibaud
 * 
 * @formatter:off
 */
public class FatJarBuild extends JkClass {

    JkPluginProject java = getPlugin(JkPluginProject.class);
    
    @JkDefImport("../dev.jeka.samples.basic")
    private JavaPluginBuild sampleBuild;

    @Override
    protected void setup() {
        java.getProject()
            .getPublication()
                .getArtifactProducer()
                    .putMainArtifact(java.getProject().getConstruction()::createFatJar)
                .__
            .__
            .simpleFacade()
                .setCompileDependencies(deps -> deps
                        .and("com.google.guava:guava:22.0")
                        .and(sampleBuild.java.getProject().toDependency()));
    }
   
}

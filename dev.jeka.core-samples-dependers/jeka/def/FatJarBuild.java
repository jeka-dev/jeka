import dev.jeka.core.samples.JavaPluginBuild;
import dev.jeka.core.tool.JkClass;
import dev.jeka.core.tool.JkDefImport;
import dev.jeka.core.tool.builtins.java.JkPluginJava;

/**
 * Simple build demonstrating of how Jeka can handle multi-project build.
 * 
 * @author Jerome Angibaud
 * 
 * @formatter:off
 */
public class FatJarBuild extends JkClass {

    JkPluginJava java = getPlugin(JkPluginJava.class);
    
    @JkDefImport("../dev.jeka.core-samples")
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

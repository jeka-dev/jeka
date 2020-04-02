import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.samples.AClassicBuild;
import dev.jeka.core.tool.JkCommandSet;
import dev.jeka.core.tool.JkDefImport;
import dev.jeka.core.tool.builtins.java.JkPluginJava;

/**
 * Simple build demonstrating of how Jerkar can handle multi-project build.
 * 
 * @author Jerome Angibaud
 * 
 * @formatter:off
 */
public class FatJarBuild extends JkCommandSet {

    JkPluginJava javaPlugin = getPlugin(JkPluginJava.class);
    
    @JkDefImport("../dev.jeka.core-samples")
    private AClassicBuild sampleBuild;

    @Override
    protected void setup() {
        javaPlugin.getProject()
            .getArtifactProducer()
                .putMainArtifact(javaPlugin.getProject().getSteps().getPackaging()::createFatJar).__
            .getDependencyManagement()
                .addDependencies(JkDependencySet.of().and(sampleBuild.javaPlugin.getProject()))
        ;
    }
   
}

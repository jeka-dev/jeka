import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.samples.AClassicBuild;
import dev.jeka.core.tool.JkCommands;
import dev.jeka.core.tool.JkImportProject;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.builtins.java.JkPluginJava;

/**
 * Simple build demonstrating of how Jerkar can handle multi-project build.
 * 
 * @author Jerome Angibaud
 * 
 * @formatter:off
 */
public class FatJarBuild extends JkCommands {

    JkPluginJava javaPlugin = getPlugin(JkPluginJava.class);
    
    @JkImportProject("../dev.jeka.core.samples")
    private AClassicBuild sampleBuild;

    @Override
    protected void setup() {
        javaPlugin.getProject().addDependencies(JkDependencySet.of().and(sampleBuild.javaPlugin.getProject()));
        javaPlugin.getProject().setSourceVersion(JkJavaVersion.V7);
        javaPlugin.getProject().getMaker().defineMainArtifactAsFatJar(true);
    } 
    
    public static void main(String[] args) {
		JkInit.instanceOf(FatJarBuild.class, "-javaPlugin#tests.fork").javaPlugin.clean().pack();
	}

   
}

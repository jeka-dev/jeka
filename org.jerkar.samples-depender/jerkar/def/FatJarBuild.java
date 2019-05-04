import org.jerkar.api.depmanagement.JkDependencySet;
import org.jerkar.api.java.JkJavaVersion;
import org.jerkar.samples.AClassicBuild;
import org.jerkar.tool.JkImportRun;
import org.jerkar.tool.JkInit;
import org.jerkar.tool.JkRun;
import org.jerkar.tool.builtins.java.JkPluginJava;

/**
 * Simple build demonstrating of how Jerkar can handle multi-project build.
 * 
 * @author Jerome Angibaud
 * 
 * @formatter:off
 */
public class FatJarBuild extends JkRun {

    JkPluginJava javaPlugin = getPlugin(JkPluginJava.class);
    
    @JkImportRun("../org.jerkar.samples")
    private AClassicBuild sampleBuild;

    @Override
    protected void setup() {
        javaPlugin.getProject().addDependencies(JkDependencySet.of()
                .and(sampleBuild.javaPlugin.getProject()));
        javaPlugin.getProject().setSourceVersion(JkJavaVersion.V7);
    } 
    
    public static void main(String[] args) {
		JkInit.instanceOf(FatJarBuild.class, "-javaPlugin#tests.fork").javaPlugin.getProject().getMaker().makeAllArtifacts();
	}

   
}

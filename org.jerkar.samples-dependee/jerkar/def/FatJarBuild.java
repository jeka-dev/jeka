import org.jerkar.api.depmanagement.JkArtifactId;
import org.jerkar.api.depmanagement.JkDependencySet;
import org.jerkar.api.java.JkJavaVersion;
import org.jerkar.samples.AClassicBuild;
import org.jerkar.tool.JkImportRun;
import org.jerkar.tool.JkInit;
import org.jerkar.tool.builtins.java.JkJavaProjectBuild;

/**
 * Simple build demonstrating of how Jerkar can handle multi-project build.
 * 
 * @author Jerome Angibaud
 * 
 * @formatter:off
 */
public class FatJarBuild extends JkJavaProjectBuild {
    
    @JkImportRun("../org.jerkar.samples")
    private AClassicBuild sampleBuild;

    @Override
    protected void setup() {
        project().addDependencies(JkDependencySet.of()
                .and(sampleBuild.project()));
        project().setSourceVersion(JkJavaVersion.V7);
    } 
    
    public static void main(String[] args) {
		JkInit.instanceOf(FatJarBuild.class, "-java#tests.fork").maker().makeAllArtifacts();
	}

   
}

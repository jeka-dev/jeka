import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.java.JkJavaVersion;
import org.jerkar.samples.MavenStyleBuild;
import org.jerkar.tool.JkImportBuild;
import org.jerkar.tool.JkInit;
import org.jerkar.tool.builtins.java.JkJavaProjectBuild;

/**
 * Simple build demonstrating how Jerkar can handle multi-project build.
 * <p>
 * Here, the project depends on the <code>org.jerkar.samples</code> sibling project.
 * More precisely, on the jar file produced by <code>MavenStyleBuild</code>.
 * <p>
 * Compilation depends on a jar produced by <code>org.jerkar.samples</code>
 * project and from its transitive dependencies.
 * 
 * @author Jerome Angibaud
 * 
 * @formatter:off
 */
public class NormalJarBuild extends JkJavaProjectBuild {

    /*
     *  Creates a sample build instance of the 'org.jerkar.samples' project.
     *  The 'samples' project path must be relative to this one.
     *  So in this case, the two project are supposed to lie accept the same folder.
     */
    @JkImportBuild("../org.jerkar.samples")
    private MavenStyleBuild sampleBuild;  

    @Override
    protected void configurePlugins() {
        java().project()
                .setDependencies(JkDependencies.of().and(sampleBuild.java().project()))
                .setSourceVersion(JkJavaVersion.V7);
    }

    public static void main(String[] args) {
        JkInit.instanceOf(NormalJarBuild.class).doDefault();
    }

}

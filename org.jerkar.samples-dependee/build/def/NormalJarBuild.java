import java.io.File;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.java.JkJavaVersion;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.samples.MavenStyleBuild;
import org.jerkar.tool.JkImportBuild;
import org.jerkar.tool.JkJavaProjectBuild;

/**
 * Simple build demonstrating how Jerkar can handle multi-project build.
 * <p>
 * Here, the project depends on the <code>org.jerkar.samples</code> project.
 * More precisely, on the jar file produced by <code>MavenStyleBuild</code>.
 * <p>
 * Compilation depends on a jar produced by <code>org.jerkar.samples</code>
 * project and from ts transitive dependencies.
 * 
 * @author Jerome Angibaud
 * 
 * @formatter:off
 */
public class NormalJarBuild extends JkJavaProjectBuild {

    /*
     *  Creates a sample build instance of the 'org.jerkar.samples' project.
     *  The 'samples' project path must be relative to this one.
     *  So in this case, the two project are supposed to lie in the same folder.
     */
    @JkImportBuild("../org.jerkar.samples")
    private MavenStyleBuild sampleBuild;  

    @Override
    protected JkJavaProject createProject(JkJavaProject project) {
        return project
                .setDependencies(JkDependencies.of(sampleBuild.project()))
                .setSourceVersion(JkJavaVersion.V7);
    }

}

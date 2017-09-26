import java.io.File;

import org.jerkar.api.depmanagement.JkArtifactFileId;
import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.java.JkJavaVersion;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.samples.AClassicBuild;
import org.jerkar.tool.JkImportBuild;
import org.jerkar.tool.JkInit;
import org.jerkar.tool.builtins.javabuild.JkJavaProjectBuild;

/**
 * Simple build demonstrating of how Jerkar can handle multi-project build.
 * <p>
 * Here, the project depends on the <code>org.jerkar.samples</code> project.
 * More precisely, on the [fat
 * jar](http://stackoverflow.com/questions/19150811/what-is-a-fat-jar) file
 * produced by the <code>AClassicalBuild</code> build.
 * <p>
 * The compilation relies on a fat jar (a jar containing all the dependencies)
 * produced by <code>org.jerkar.samples</code> project. The build produces in
 * turns, produces a fat jar merging the fat jar dependency, the classes of this
 * project and its module dependencies.
 * 
 * @author Jerome Angibaud
 * 
 * @formatter:off
 */
public class FatJarBuild extends JkJavaProjectBuild {
    
    @JkImportBuild("../org.jerkar.samples")
    private AClassicBuild sampleBuild;
    
    @Override
    protected JkJavaProject createProject(JkJavaProject project) {
        return project
                .setDependencies(JkDependencies.of(sampleBuild.project(), JkArtifactFileId.of("fat", "jar")))
                .setSourceVersion(JkJavaVersion.V7);
    } 
    
    public static void main(String[] args) {
		JkInit.instanceOf(FatJarBuild.class, args).doDefault();
	}

   
}

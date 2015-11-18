import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.samples.MavenStyleBuild;
import org.jerkar.tool.JkProject;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;

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
public class NormalJarBuild extends JkJavaBuild {

    /*
     *  Creates a sample build instance of the 'org.jerkar.samples' project.
     *  The 'samples' project path must be relative to this one.
     *  So in this case, the two project are supposed to lie in the same folder.
     */
    @JkProject("../org.jerkar.samples")
    private MavenStyleBuild sampleBuild;  

    @Override
    protected JkDependencies dependencies() {
	return JkDependencies
		
		// Depends on the jar file produced by the mavenBuildStyle of the 'sample' project
		// When fetching the dependencies, if the jar file in the 'samples' project is not present,
		// then a 'sampleBuild' is launched in order to produce it.
		// The 'sampleBuild' is launched with the 'doDefault' method unless you specify another ones
		.on(COMPILE, sampleBuild.asDependency(sampleBuild.packer().jarFile())) 
		
		// Depends on the transitive build defined in the mavenBuildStyle of the 'sample' project
		.and(COMPILE, sampleBuild.depsFor(COMPILE))
		
		// Additional dependency
		.and(RUNTIME, "ch.qos.logback:logback-classic:1.+");
    }

}

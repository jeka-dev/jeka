import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.java.JkJavaCompiler;
import org.jerkar.samples.AClassicBuild;
import org.jerkar.tool.JkInit;
import org.jerkar.tool.JkProject;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;

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
public class FatJarBuild extends JkJavaBuild {
	
    /*
     *  Creates a sample build instance of the 'org.jerkar.samples' project.
     *  The 'samples' project path must be relative to this one.
     *  So in this case, the two project are supposed to lie in the same folder.
     */
    @JkProject("../org.jerkar.samples")
    private AClassicBuild sampleBuild;

    @Override
    protected void init() {
    	sampleBuild.pack.fatJar = true; // Tell the dependency build to generate a fat jar 
    	pack.fatJar = true; // Tell this build to generate a fat jar as well
    }
    
    @Override
	public String javaSourceVersion() {
		return JkJavaCompiler.V7;
	}

    @Override
    protected JkDependencies dependencies() {

	// Tell this project depends of the fat jar produced by 'AClassicBuild'
	// from project '../org.jerkar.samples'
	return JkDependencies
		
		// Depends on the fat jar file produced by the mavenBuildStyle of the 'sample' project
		// When fetching the dependencies, if the fat jar file in the 'samples' project is not present,
		// then a 'sampleBuild' is launched in order to produce it.
		// The 'sampleBuild' is launched with the 'doDefault' method unless you specify another ones
		.of(COMPILE, sampleBuild.asDependency(sampleBuild.packer().fatJarFile()))
		
		// Extra dependency
		.and("com.google.guava:guava", "18.0", PROVIDED);
    }
    
    public static void main(String[] args) {
		JkInit.instanceOf(FatJarBuild.class, args).doDefault();
	}

}

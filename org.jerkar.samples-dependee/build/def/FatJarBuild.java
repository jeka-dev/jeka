import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.samples.AClassicBuild;
import org.jerkar.tool.JkProject;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;

/**
 * Simple build demonstrating how Jerkar can handle multi-project build.<p>
 * Here, the project depends on the <code>org.jerkar.samples</code> project. 
 * More precisely, on the fat jar file produced by the <code>AClassicalBuild</code> build.
 * <p>
 * The compilation relies on a fat jar (a jar containing all the dependencies) produced by <code>org.jerkar.samples</code> project.
 * The build produces in turns a fat jar merging the fat jar dependency, the classes of this project and its module dependencies.
 * 
 * @author Jerome Angibaud
 */
public class FatJarBuild extends JkJavaBuild {
	
	@JkProject("../org.jerkar.samples")
	private AClassicBuild sampleBuild;
	
	@Override
	protected void init() {
		sampleBuild.pack.fatJar = true; // Tell the dependency build to generate a fat jar
		this.pack.fatJar = true; // Tell this build to generate a fat jar
	}

	
	@Override
	protected JkDependencies dependencies() {
		
		// Tell this project depends of the fat jar produced by 'AClassicBuild' 
		// from project '../org.jerkar.samples'
		return JkDependencies
				.on(COMPILE, sampleBuild.asDependency(sampleBuild.packer().fatJarFile()))
				.and(RUNTIME, "ch.qos.logback:logback-classic:1.+");
	}
	
	
}

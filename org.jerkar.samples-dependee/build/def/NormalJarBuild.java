import javax.swing.text.StyleContext.SmallAttributeSet;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkScope;
import org.jerkar.samples.AClassicBuild;
import org.jerkar.samples.MavenStyleBuild;
import org.jerkar.tool.JkProject;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;

/**
 * Simple build demonstrating how Jerkar can handle multi-project build.<p>
 * Here, the project depends on the <code>org.jerkar.samples</code> project. 
 * More precisely, on the fat jar file produced by <code>MavenStyleBuild</code>.
 * <p>
 * Compilation depends on a jar produced by <code>org.jerkar.samples</code> project and 
 * The build produces in turns a fat jar merging the fat jar dependency, the classes of this project and its module dependencies.
 * 
 * @author Jerome Angibaud
 */
public class NormalJarBuild extends JkJavaBuild {
	
	@JkProject("../org.jerkar.samples")
	private MavenStyleBuild sampleBuild;
	
	@Override
	protected void init() {
		this.pack.fatJar = true; // Tell this build to generate a fat jar
	}

	
	@Override
	protected JkDependencies dependencies() {
		
		// Tell this project depends of the jar produced by 'MavenStyleBuild' 
		// from project '../org.jerkar.samples' and its COMPILE dependencies
		return JkDependencies
				.on(COMPILE, sampleBuild.asDependency(sampleBuild.packer().jarFile()))
				.and(COMPILE, sampleBuild.depsFor(COMPILE))
				.and(RUNTIME, "ch.qos.logback:logback-classic:1.+");
	}
	
	
}

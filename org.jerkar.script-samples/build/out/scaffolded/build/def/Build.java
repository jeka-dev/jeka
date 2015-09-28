import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.tool.JkBuildDependencySupport;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;


class Build extends JkJavaBuild {
	
	@Override
	protected void init() {
	    // Add you init code here (if needed)
	    // You can remove this method declaration if you don't do anything special at initialization.

	}
	
	@Override
	protected JkDependencies dependencies() {
		return JkDependencies.builder()
			// Add your dependencies here
			// You can remove this method declaration if you don't use managed or external project dependencies

		.build();
	}
	
	

}

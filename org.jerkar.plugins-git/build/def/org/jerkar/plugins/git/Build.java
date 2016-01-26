package org.jerkar.plugins.git;

import org.jerkar.depmanagement.JkDependencies;
import org.jerkar.JkBuild;
import org.jerkar.builtins.javabuild.JkJavaBuild;


class Build extends JkJavaBuild {
	
	@Override
	protected void init() {
	    // Add you init code here (if needed)

	}
	
	@Override
	protected JkDependencies dependencies() {
		return JkDependencies.builder()
			// Add your dependencies here

		.build();
	}
	
	

}

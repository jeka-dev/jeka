package org.jerkar.plugins.sonar;

import org.jerkar.CoreBuild;
import org.jerkar.JerkarBuild;
import org.jerkar.JkProject;
import org.jerkar.depmanagement.JkDependencies;

public class PluginsSonarBuild extends JerkarBuild {
		
	@JkProject("../org.jerkar.core")
	private CoreBuild core;
	
	@Override
	protected JkDependencies dependencies() {
		return JkDependencies.onProject(PROVIDED, core, core.packer().jarFile());
	}	

}

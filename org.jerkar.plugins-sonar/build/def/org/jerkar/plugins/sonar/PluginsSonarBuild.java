package org.jerkar.plugins.sonar;

import org.jerkar.CoreBuild;
import org.jerkar.JerkarBuild;
import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.tool.JkProject;

public class PluginsSonarBuild extends JerkarBuild {
		
	@JkProject("../org.jerkar.core")
	private CoreBuild core;
	
	@Override
	protected JkDependencies dependencies() {
		return JkDependencies.onProject(PROVIDED, core, core.packer().jarFile());
	}	

}

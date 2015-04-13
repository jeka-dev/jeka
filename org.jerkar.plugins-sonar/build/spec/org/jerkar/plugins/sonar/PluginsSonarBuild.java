package org.jerkar.plugins.sonar;

import org.jerkar.CoreBuild;
import org.jerkar.JkProject;
import org.jerkar.depmanagement.JkDependencies;
import org.jerkar.java.build.JkJavaBuild;

public class PluginsSonarBuild extends JkJavaBuild {
		
	@JkProject("../org.jerkar.core")
	private CoreBuild core;
	
	@Override
	protected JkDependencies dependencies() {
		return JkDependencies.onProject(PROVIDED, core, core.packer().jarFile());
	}	

}

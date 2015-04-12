package org.jerkar.plugins.sonar;

import org.jerkar.CoreBuild;
import org.jerkar.JakeProject;
import org.jerkar.depmanagement.JkDependencies;
import org.jerkar.java.build.JkJavaBuild;

public class PluginsSonarBuild extends JkJavaBuild {
		
	@JakeProject("../org.jerkar.core")
	private CoreBuild core;
	
	@Override
	protected JkDependencies dependencies() {
		return JkDependencies.onProject(PROVIDED, core, core.packer().jarFile());
	}	

}

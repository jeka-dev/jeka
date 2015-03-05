package org.jake.plugins.sonar;

import org.jake.CoreBuild;
import org.jake.JakeProject;
import org.jake.depmanagement.JakeDependencies;
import org.jake.java.build.JakeJavaBuild;

public class PluginsSonarBuild extends JakeJavaBuild {
		
	@JakeProject("../org.jake.core")
	private CoreBuild core;
	
	@Override
	protected JakeDependencies dependencies() {
		return JakeDependencies.onProject(PROVIDED, core, core.packer().jarFile());
	}	

}

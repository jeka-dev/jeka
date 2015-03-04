package org.jake.plugins.sonar;

import org.jake.depmanagement.JakeDependencies;
import org.jake.java.build.JakeJavaBuild;

public class PluginsSonarBuild extends JakeJavaBuild {
		
	private final JakeJavaBuild core = relativeProject(JakeJavaBuild.class, "../org.jake.core");
	
	@Override
	protected JakeDependencies dependencies() {
		return JakeDependencies.onProject(PROVIDED, core, core.packer().jarFile());
	}	

}

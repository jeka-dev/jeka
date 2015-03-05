package org.jake.plugins.jacoco;

import org.jake.CoreBuild;
import org.jake.JakeProject;
import org.jake.depmanagement.JakeDependencies;
import org.jake.java.build.JakeJavaBuild;


public class PluginsJakeocoBuild extends JakeJavaBuild {
	
	@JakeProject("../org.jake.core")
	public CoreBuild core;
	
	@Override
	protected JakeDependencies dependencies() {
		return JakeDependencies
			.onProject(PROVIDED, core , core.packer().jarFile())
			.andExternal(TEST, "junit:junit:4.11");
	}
	
	public static void main(String[] args) {
		new PluginsJakeocoBuild().base();
	}
	
	
}

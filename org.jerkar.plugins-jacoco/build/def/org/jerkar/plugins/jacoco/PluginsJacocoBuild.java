package org.jerkar.plugins.jacoco;

import org.jerkar.CoreBuild;
import org.jerkar.JerkarBuild;
import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.tool.JkProject;


public class PluginsJacocoBuild extends JerkarBuild {
	
	@JkProject("../org.jerkar.core")
	public CoreBuild core;
	
	@Override
	protected JkDependencies dependencies() {
		return JkDependencies
			.onProject(PROVIDED, core , core.packer().jarFile())
			//.andExternal(TEST, "junit:junit:4.11")
			.andFiles(TEST, core.baseDir("build/libs/provided/junit-4.11.jar"), 
					core.baseDir("build/libs/provided/hamcrest-core-1.3.jar"));
	}
	
	public static void main(String[] args) {
		new PluginsJacocoBuild().doDefault();
	}
	
	
}

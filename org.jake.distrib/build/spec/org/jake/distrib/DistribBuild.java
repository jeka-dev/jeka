package org.jake.distrib;

import java.io.File;

import org.jake.CoreBuild;
import org.jake.JakeBuild;
import org.jake.JakeDir;
import org.jake.JakeProject;
import org.jake.plugins.jacoco.PluginsJakeocoBuild;
import org.jake.plugins.sonar.PluginsSonarBuild;

public class DistribBuild extends JakeBuild {
	
	@JakeProject("../org.jake.core")
	private CoreBuild core;
	
	@JakeProject("../org.jake.plugins-sonar")
	private PluginsSonarBuild pluginsSonar;
	
	@JakeProject("../org.jake.plugins-jacoco")
	private PluginsJakeocoBuild pluginsJacoco;
	
	@Override
	public void base() {
		core.base();
		pluginsJacoco.base();
		pluginsSonar.base();
		JakeDir.of(core.distFolder()).copyTo(localDist());
		
	}
	
	public File localDist() {
		return this.ouputDir("dist");
	}
	

}

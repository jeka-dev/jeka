package org.jake.distrib;

import java.io.File;

import org.jake.CoreBuild;
import org.jake.JakeBuild;
import org.jake.JakeDir;
import org.jake.JakeLog;
import org.jake.JakeProject;
import org.jake.JakeZipper;
import org.jake.plugins.jacoco.PluginsJakeocoBuild;
import org.jake.plugins.sonar.PluginsSonarBuild;

public class DistribBuild extends JakeBuild {
	
	@JakeProject("../org.jake.plugins-sonar")
	private PluginsSonarBuild pluginsSonar;
	
	@JakeProject("../org.jake.plugins-jacoco")
	private PluginsJakeocoBuild pluginsJacoco;
	
	
	@Override
	public void base() {
		super.base();
	}
	
	public void distrib() {
		
		// build dependee projects
		this.buildDependencies().invokeOnAllTransitiveBase();
		
		JakeLog.startln("Creating distribution file");
		
		// copy core distribution locally
		CoreBuild core = pluginsJacoco.core;  // The core project is got by transitivity
		JakeDir dist = JakeDir.of(this.ouputDir("dist")).copyDirContent(core.distribFolder);
		
		// Add plugins to the distribution
		JakeDir ext = dist.sub("libs/ext").copyFiles(pluginsSonar.packer().jarFile(), pluginsJacoco.packer().jarFile());
		dist.sub("libs/sources").copyFiles(pluginsSonar.packer().jarSourceFile(), pluginsJacoco.packer().jarSourceFile());
		
		// add plugins to the fat jar
		File fat = dist.file(core.packer().fatJarFile().getName());
		JakeZipper.of().merge(ext.include("**/*.jar")).appendTo(fat);
		
		// pack all
		dist.zip().to(this.ouputDir("jake-distrib.zip"));
		
		JakeLog.done();
	}
	
	

}

package org.jake.distrib;

import java.io.File;

import org.jake.CoreBuild;
import org.jake.JakeBuild;
import org.jake.JakeDir;
import org.jake.JakeDoc;
import org.jake.JakeLog;
import org.jake.JakeProject;
import org.jake.JakeZipper;
import org.jake.plugins.jacoco.PluginsJakeocoBuild;
import org.jake.plugins.sonar.PluginsSonarBuild;

public class DistribBuild extends JakeBuild {
	
	@JakeProject("../org.jake.plugins-sonar")
	PluginsSonarBuild pluginsSonar;
	
	@JakeProject("../org.jake.plugins-jacoco")
	PluginsJakeocoBuild pluginsJacoco;
	
	@JakeDoc("Construct a distrib assuming all dependent sub projects are already built.")
	public void distrib() {
		
		JakeLog.startln("Creating distribution file");
		
		// copy core distribution locally
		CoreBuild core = pluginsJacoco.core;  // The core project is got by transitivity
		JakeDir dist = JakeDir.of(this.ouputDir("dist")).copyInDirContent(core.distribFolder);
		
		// Add plugins to the distribution
		JakeDir ext = dist.sub("libs/ext").importFiles(pluginsSonar.packer().jarFile(), pluginsJacoco.packer().jarFile());
		dist.sub("libs/sources").importFiles(pluginsSonar.packer().jarSourceFile(), pluginsJacoco.packer().jarSourceFile());
		
		// add plugins to the fat jar
		File fat = dist.file(core.packer().fatJarFile().getName());
		JakeZipper.of().merge(ext.include("**/*.jar")).appendTo(fat);
		
		// pack all
		dist.zip().to(ouputDir("jake-distrib.zip"));
		
		JakeLog.done();
	}
	
	@JakeDoc("End to end method to construct a distrib.")
	public void doDistrib() {
		base();
		buildDependencies().invokeBaseOnAllSubProjects();
		distrib();
	} 
	
	

}

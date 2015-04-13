package org.jerkar.distrib.all;

import java.io.File;

import org.jerkar.CoreBuild;
import org.jerkar.JkBuild;
import org.jerkar.JkDir;
import org.jerkar.JkDoc;
import org.jerkar.JkLog;
import org.jerkar.JkProject;
import org.jerkar.JkZipper;
import org.jerkar.plugins.jacoco.PluginsJacocoBuild;
import org.jerkar.plugins.sonar.PluginsSonarBuild;

public class DistribAllBuild extends JkBuild {
	
	@JkProject("../org.jerkar.plugins-sonar")
	PluginsSonarBuild pluginsSonar;
	
	@JkProject("../org.jerkar.plugins-jacoco")
	PluginsJacocoBuild pluginsJacoco;
	
	@JkDoc("Construct a distrib assuming all dependent sub projects are already built.")
	public void distrib() {
		
		JkLog.startln("Creating distribution file");
		
		JkLog.info("Copy core distribution localy.");
		CoreBuild core = pluginsJacoco.core;  // The core project is got by transitivity
		JkDir dist = JkDir.of(this.ouputDir("dist")).importDirContent(core.distribFolder);
		
		JkLog.info("Add plugins to the distribution");
		JkDir ext = dist.sub("libs/ext").importFiles(pluginsSonar.packer().jarFile(), pluginsJacoco.packer().jarFile());
		dist.sub("libs/sources").importFiles(pluginsSonar.packer().jarSourceFile(), pluginsJacoco.packer().jarSourceFile());
		
		JkLog.info("Add plugins to the fat jar.");
		File fat = dist.file(core.packer().fatJarFile().getName());
		JkZipper.of().merge(ext.include("**/*.jar")).appendTo(fat);
		
		JkLog.info("Pack all");
		dist.zip().to(ouputDir("jerkar-distrib.zip"));
		
		JkLog.done();
	}
	
	@JkDoc("End to end method to construct a distrib.")
	public void doDefault() {
		buildDependencies().invokeDoDefaultMethodOnAllSubProjects();
		distrib();
	} 
	
	
}

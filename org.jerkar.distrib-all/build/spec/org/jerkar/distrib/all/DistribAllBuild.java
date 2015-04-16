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
import org.jerkar.utils.JkUtilsFile;
import org.jerkar.utils.JkUtilsIO;

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
		File distDir = this.ouputDir("dist");
		JkDir dist = JkDir.of(distDir).importDirContent(core.distribFolder);
		String content = JkUtilsFile.read(new File(core.distribFolder, "jerkar.bat"))
				.replace("org.jerkar.core.jar", "org.jerkar.core-fat.jar");
		File batFile = new File(distDir, "jerkar.bat");
		JkUtilsFile.writeString(batFile, content, false);
		
		
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

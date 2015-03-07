package org.jake.plugins.jacoco;


import org.jake.depmanagement.JakeRepos;
import org.jake.plugins.jacoco.JakeBuildPluginJacoco;

/**
 * The purpose of this build class is just for testing the plugin itself in the Ide.
 * It can't be compiled by Jake cause it refers to classes file from this project.
 * That's why its name start with '_' (java source starting with '_' are not compiled by Jake).
 */
public class _JakeocoBuildDeclaringPlugin extends PluginsJakeocoBuild {
	
	public static void main(String[] args) {
		_JakeocoBuildDeclaringPlugin build = new _JakeocoBuildDeclaringPlugin();
		build.plugins.addActivated(new JakeBuildPluginJacoco());
		build.base();
	}
	
	@Override
	protected JakeRepos downloadRepositories() {
		return super.downloadRepositories().andMaven("http://i-net1102e-prod:8081/nexus/service/local/repo_groups/bnppf-secured");
	}
	
	
	
}

package org.jake.plugins.jacoco.sample;

import org.jake.depmanagement.JakeRepos;
import org.jake.plugins.jacoco.JakeocoBuild;
import org.jake.plugins.jacoco.JakeocoJakeJavaBuildPlugin;

/**
 * The purpose of this build class is just for testing the plugin itself in the Ide.
 * It can't be compiled by Jake cause it refers to classes file from this project
 */
public class JakeocoBuildDeclaringPlugin extends JakeocoBuild {
	
	public static void main(String[] args) {
		JakeocoBuildDeclaringPlugin build = new JakeocoBuildDeclaringPlugin();
		build.plugins.addActivated(new JakeocoJakeJavaBuildPlugin());
		build.base();
	}
	
	@Override
	protected JakeRepos downloadRepositories() {
		return super.downloadRepositories().andMaven("http://i-net1102e-prod:8081/nexus/service/local/repo_groups/bnppf-secured");
	}
	
	
	
}

package org.jake.plugins.jacoco.sample;

import org.jake.plugins.jacoco.JakeocoBuild;
import org.jake.plugins.jacoco.JakeocoJakeJavaBuildPlugin;

/**
 * The purpose of this build class is just for testing the plugin itself
 */
public class JakeocoBuildDeclaringPlugin extends JakeocoBuild {
	
	
	{
		downloadRepoUrl = "http://i-net1102e-prod:8081/nexus/service/local/repo_groups/bnppf-secured";
		plugins.addActivated(new JakeocoJakeJavaBuildPlugin());
		
	}
	
	public static void main(String[] args) {
		new JakeocoBuildDeclaringPlugin().base();
	}
	
	
	
}

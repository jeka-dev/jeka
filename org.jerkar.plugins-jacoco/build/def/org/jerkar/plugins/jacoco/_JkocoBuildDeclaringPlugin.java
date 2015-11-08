package org.jerkar.plugins.jacoco;

import org.jerkar.api.depmanagement.JkRepos;
import org.jerkar.plugins.jacoco.JkBuildPluginJacoco;

/**
 * The purpose of this build class is just for testing the plugin itself in the
 * Ide. It can't be compiled by Jerkar cause it refers to classes file from this
 * project. That's why its name start with '_' (java source starting with '_'
 * are not compiled by Jerkar).
 */
public class _JkocoBuildDeclaringPlugin extends PluginsJacocoBuild {

    public static void main(String[] args) {
	_JkocoBuildDeclaringPlugin build = new _JkocoBuildDeclaringPlugin();
	build.plugins.activate(new JkBuildPluginJacoco());
	build.doDefault();
    }

    @Override
    protected JkRepos downloadRepositories() {
	return super.downloadRepositories()
		.andMaven("http://i-net1102e-prod:8081/nexus/service/local/repo_groups/bnppf-secured");
    }

}

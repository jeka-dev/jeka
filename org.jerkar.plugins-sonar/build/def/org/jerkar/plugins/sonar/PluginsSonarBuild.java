package org.jerkar.plugins.sonar;

import org.jerkar.AbstractBuild;
import org.jerkar.CoreBuild;
import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.tool.JkProject;

public class PluginsSonarBuild extends AbstractBuild {

    @JkProject("../org.jerkar.core")
    private CoreBuild core;

    @Override
    protected JkDependencies dependencies() {
	return JkDependencies.of(PROVIDED, core.asDependency(core.packer().jarFile()));
    }

}

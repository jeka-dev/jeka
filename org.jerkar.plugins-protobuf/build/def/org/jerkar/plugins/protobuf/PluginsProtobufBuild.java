package org.jerkar.plugins.protobuf;


import org.jerkar.AbstractBuild;
import org.jerkar.CoreBuild;
import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.tool.JkProject;

public class PluginsProtobufBuild extends AbstractBuild {

    @JkProject("../org.jerkar.core")
    private CoreBuild core;

    @Override
    protected JkDependencies dependencies() {
	return JkDependencies.of(PROVIDED, core.asDependency(core.packer().jarFile()));
    }

}

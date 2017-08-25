package org.jerkar.plugins.sonar;

import org.jerkar.AbstractBuild;
import org.jerkar.CoreBuild;
import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.tool.JkInit;
import org.jerkar.tool.JkProject;

public class PluginsSonarBuild extends AbstractBuild {

    @JkProject("../org.jerkar.core")
    private CoreBuild core;

    public static void main(String[] args) {
        JkInit.instanceOf(PluginsSonarBuild.class, args).doDefault();
    }

    @Override
    public JkDependencies dependencies() {
        return JkDependencies.of(PROVIDED, core.asDependency(core.packer().jarFile()));
    }

}

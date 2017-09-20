package org.jerkar.plugins.sonar;

import org.jerkar.AbstractBuild;
import org.jerkar.CoreBuild;
import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.tool.JkImportBuild;
import org.jerkar.tool.JkInit;

public class PluginsSonarBuild extends AbstractBuild {

    @JkImportBuild("../org.jerkar.core")
    private CoreBuild core;

    public static void main(String[] args) {
        JkInit.instanceOf(PluginsSonarBuild.class, args).doDefault();
    }

    @Override
    public JkDependencies dependencies() {
        return JkDependencies.of(PROVIDED, core.asDependency(core.packer().jarFile()));
    }

}

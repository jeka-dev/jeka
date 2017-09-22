package org.jerkar.plugins.sonar;

import org.jerkar.OldAbstractBuild;
import org.jerkar.OldCoreBuild;
import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.tool.JkImportBuild;
import org.jerkar.tool.JkInit;

public class OldPluginsSonarBuild extends OldAbstractBuild {

    @JkImportBuild("../org.jerkar.core")
    private OldCoreBuild core;

    public static void main(String[] args) {
        JkInit.instanceOf(OldPluginsSonarBuild.class, args).doDefault();
    }

    @Override
    public JkDependencies dependencies() {
        return JkDependencies.of(PROVIDED, core.asDependency(core.packer().jarFile()));
    }

}

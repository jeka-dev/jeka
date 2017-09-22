package org.jerkar.plugins.jacoco;

import org.jerkar.OldAbstractBuild;
import org.jerkar.OldCoreBuild;
import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.tool.JkImportBuild;

public class OldPluginsJacocoBuild extends OldAbstractBuild {

    @JkImportBuild("../org.jerkar.core")
    public OldCoreBuild core;

    public static void main(String[] args) {
        new OldPluginsJacocoBuild().doDefault();
    }

    @Override
    public JkDependencies dependencies() {
        return JkDependencies.builder().on(core.asDependency(core.packer().jarFile())).scope(PROVIDED)
                .on(core.file("build/libs/provided/junit-4.11.jar"),
                        core.file("build/libs/provided/hamcrest-core-1.3.jar"))
                .scope(TEST).build();
    }

}

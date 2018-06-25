package org.jerkar.plugins.jacoco;

import static org.jerkar.api.depmanagement.JkJavaDepScopes.PROVIDED;
import static org.jerkar.api.depmanagement.JkJavaDepScopes.TEST;

import org.jerkar.CoreBuild;
import org.jerkar.api.depmanagement.JkDependencySet;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.tool.JkImportBuild;
import org.jerkar.tool.builtins.java.JkJavaProjectBuild;

public class PluginsJacocoBuild extends JkJavaProjectBuild {

    @JkImportBuild("../org.jerkar.core")
    public CoreBuild core;

    @Override
    protected void configurePlugins() {
        JkJavaProject project = java().project();
        CoreBuild.applyCommonSettings(project, "plugins-jacoco");
        project.setDependencies(JkDependencySet.of()
                .and(core.java().project(), PROVIDED)
                .and(core.baseDir().resolve("build/libs/provided/junit-4.11.jar"), TEST)
                .and(core.baseDir().resolve("build/libs/provided/hamcrest-core-1.3.jar"), TEST));
    }

    public static void main(String[] args) {
        new PluginsJacocoBuild().doDefault();
    }

}

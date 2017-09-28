package org.jerkar.plugins.jacoco;

import static org.jerkar.api.depmanagement.JkJavaDepScopes.PROVIDED;
import static org.jerkar.api.depmanagement.JkJavaDepScopes.TEST;

import org.jerkar.CoreBuild;
import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.tool.JkImportBuild;
import org.jerkar.tool.builtins.java.JkJavaProjectBuild;

public class PluginsJacocoBuild extends JkJavaProjectBuild {

    @JkImportBuild("../org.jerkar.core")
    public CoreBuild core;

    @Override
    protected JkJavaProject createProject(JkJavaProject project) {
        CoreBuild.applyCommons(project, "plugins-jacoco");
        project.setDependencies(JkDependencies.builder()
                .on(core.project()).scope(PROVIDED)
                .on(core.baseDir().resolve("build/libs/provided/junit-4.11.jar")).scope(TEST)
                .on(core.baseDir().resolve("build/libs/provided/hamcrest-core-1.3.jar")).scope(TEST)
                .build());
        return project;
    }

    public static void main(String[] args) {
        new PluginsJacocoBuild().doDefault();
    }

}

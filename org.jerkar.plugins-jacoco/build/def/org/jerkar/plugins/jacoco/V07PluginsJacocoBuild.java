package org.jerkar.plugins.jacoco;

import org.jerkar.V07CoreBuild;
import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.tool.JkImportBuild;
import org.jerkar.tool.builtins.javabuild.JkJavaProjectBuild;

import java.io.File;

import static org.jerkar.api.depmanagement.JkJavaDepScopes.PROVIDED;
import static org.jerkar.api.depmanagement.JkJavaDepScopes.TEST;

public class V07PluginsJacocoBuild extends JkJavaProjectBuild {

    @JkImportBuild("../org.jerkar.core")
    public V07CoreBuild core;

    @Override
    protected JkJavaProject createProject(File baseDir) {
        JkJavaProject project = new JkJavaProject(baseDir);
        V07CoreBuild.apply(project, "plugins-jacoco");
        project.setDependencies(JkDependencies.builder()
                .on(core.project()).scope(PROVIDED)
                .on(core.file("build/libs/provided/junit-4.11.jar")).scope(TEST)
                .on(core.file("build/libs/provided/hamcrest-core-1.3.jar")).scope(TEST)
                .build());
        return project;
    }


    public static void main(String[] args) {
        new V07PluginsJacocoBuild().doDefault();
    }

}

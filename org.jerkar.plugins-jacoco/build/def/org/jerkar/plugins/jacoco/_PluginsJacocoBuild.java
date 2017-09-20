package org.jerkar.plugins.jacoco;

import org.jerkar.V07CoreBuild;
import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.tool.JkProject;
import org.jerkar.tool.builtins.javabuild.JkJavaJarBuild;

import java.io.File;

import static org.jerkar.api.depmanagement.JkJavaDepScopes.PROVIDED;
import static org.jerkar.api.depmanagement.JkJavaDepScopes.TEST;

public class _PluginsJacocoBuild extends JkJavaJarBuild {

    @JkProject("../org.jerkar.core")
    public V07CoreBuild core;

    @Override
    protected JkJavaProject createProject(File baseDir) {
        JkJavaProject project = new JkJavaProject(baseDir);
        project.setDependencies(JkDependencies.builder()
                .on(core.project().asDependency()).scope(PROVIDED)
                .on(core.file("build/libs/provided/junit-4.11.jar")).scope(TEST)
                .on(core.file("build/libs/provided/hamcrest-core-1.3.jar")).scope(TEST)
                .build());
        return project;
    }


    public static void main(String[] args) {
        new _PluginsJacocoBuild().doDefault();
    }

}

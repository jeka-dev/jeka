package org.jerkar.plugins.sonar;


import org.jerkar._CoreBuild;
import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.tool.JkInit;
import org.jerkar.tool.JkProject;
import org.jerkar.tool.builtins.javabuild.JkJavaJarBuild;

import java.io.File;

import static org.jerkar.api.depmanagement.JkJavaDepScopes.PROVIDED;

public class _PluginsSonarBuild extends JkJavaJarBuild {

    @JkProject("../org.jerkar.core")
    private _CoreBuild core;

    @Override
    protected JkJavaProject createProject(File baseDir) {
        JkJavaProject project = new JkJavaProject(baseDir);
        project.setDependencies(JkDependencies.of(PROVIDED, core.project().asDependency()));
        return project;
    }

    public static void main(String[] args) {
        JkInit.instanceOf(_PluginsSonarBuild.class, args).doDefault();
    }

}

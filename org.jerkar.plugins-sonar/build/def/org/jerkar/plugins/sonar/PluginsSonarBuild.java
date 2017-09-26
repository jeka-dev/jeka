package org.jerkar.plugins.sonar;

import org.jerkar.CoreBuild;
import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.JkImportBuild;
import org.jerkar.tool.JkInit;
import org.jerkar.tool.builtins.javabuild.JkJavaProjectBuild;

import java.io.File;

import static org.jerkar.api.depmanagement.JkJavaDepScopes.PROVIDED;

public class PluginsSonarBuild extends JkJavaProjectBuild {

    @JkImportBuild("../org.jerkar.core")
    private CoreBuild core;

    @Override
    protected JkJavaProject createProject(JkJavaProject project) {
        CoreBuild.applyCommons(project, "plugins-sonar");
        project.setDependencies(JkDependencies.builder()
                .on(core.project()).scope(PROVIDED)
                .build());
        return project;
    }

    public static void main(String[] args) {
        JkInit.instanceOf(PluginsSonarBuild.class, args).doDefault();
    }

}

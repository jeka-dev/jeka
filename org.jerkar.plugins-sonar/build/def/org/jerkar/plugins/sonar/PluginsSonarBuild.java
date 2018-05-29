package org.jerkar.plugins.sonar;

import static org.jerkar.api.depmanagement.JkJavaDepScopes.PROVIDED;

import org.jerkar.CoreBuild;
import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.tool.JkImportBuild;
import org.jerkar.tool.JkInit;
import org.jerkar.tool.builtins.java.JkJavaProjectBuild;

public class PluginsSonarBuild extends JkJavaProjectBuild {

    @JkImportBuild("../org.jerkar.core")
    private CoreBuild core;

    @Override
    protected JkJavaProject createProject() {
        final JkJavaProject project = defaultProject();
        CoreBuild.applyCommonSettings(project, "plugins-sonar");
        project.setDependencies(JkDependencies.builder()
                .on(core.project()).scope(PROVIDED)
                .build());
        return project;
    }

    public static void main(String[] args) {
        JkInit.instanceOf(PluginsSonarBuild.class, args).doDefault();
    }

}

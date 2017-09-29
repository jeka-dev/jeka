package org.jerkar.samples;

import static org.jerkar.api.depmanagement.JkPopularModules.GUAVA;
import static org.jerkar.api.depmanagement.JkPopularModules.JUNIT;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkJavaDepScopes;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.plugins.jacoco.JkPluginJacoco;
import org.jerkar.tool.JkInit;
import org.jerkar.tool.builtins.java.JkJavaProjectBuild;

/**
 * This build deleteArtifacts, compile,test launch sonar analyse by default.
 */
public class JacocoPluginBuild extends JkJavaProjectBuild {
    
    @Override
    protected JkJavaProject createProject() {
        return defaultProject()
                .setDependencies(JkDependencies.builder()
                .on(GUAVA, "18.0")
                .on(JUNIT, "4.11", JkJavaDepScopes.TEST).build());
    }

    @Override
    protected void setupPlugins() {
        new JkPluginJacoco().apply(this);
    }

    public static void main(String[] args) {
        JkInit.instanceOf(JacocoPluginBuild.class, args).project().maker().test();
    }

}

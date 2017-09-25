package org.jerkar.samples;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkJavaDepScopes;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.plugins.jacoco.JkBuildPlugin2Jacoco;
import org.jerkar.plugins.sonar.JkBuildPlugin2Sonar;
import org.jerkar.plugins.sonar.JkSonar;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.JkInit;
import org.jerkar.tool.builtins.javabuild.JkJavaProjectBuild;

import java.io.File;

import static org.jerkar.api.depmanagement.JkPopularModules.GUAVA;
import static org.jerkar.api.depmanagement.JkPopularModules.JUNIT;

/**
 * This build deleteArtifacts, compile,test launch sonar analyse by default.
 */
public class JacocoPluginBuild extends JkJavaProjectBuild {
    
    @Override
    protected JkJavaProject createProject(JkJavaProject project) {
        return project
                .setDependencies(JkDependencies.builder()
                .on(GUAVA, "18.0")
                .on(JUNIT, "4.11", JkJavaDepScopes.TEST).build());
    }

    @Override
    protected void setupPlugins() {
        new JkBuildPlugin2Jacoco().apply(this);
    }

    public static void main(String[] args) {
        JkInit.instanceOf(JacocoPluginBuild.class, args).project().maker().test();
    }

}

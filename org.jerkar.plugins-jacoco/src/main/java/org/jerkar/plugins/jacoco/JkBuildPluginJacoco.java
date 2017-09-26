package org.jerkar.plugins.jacoco;

import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.tool.JkBuild;
import org.jerkar.tool.JkBuildPlugin;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.builtins.javabuild.JkJavaProjectBuild;

@JkDoc("Modify JkJavaProject in order it runs unit tests with Jacoco agent coverage test tool. " +
        "It results is production of a coverage report file.")
public class JkBuildPluginJacoco implements JkBuildPlugin {

    @Override
    public void apply(JkBuild build) {
        if (! (build instanceof JkJavaProjectBuild)) {
            return;
        }
        final JkJavaProject project = ((JkJavaProjectBuild) build).project();
        final JkocoJunitEnhancer junitEnhancer = JkocoJunitEnhancer.of(project.getOutLayout()
              .outputFile("jacoco/jacoco.exec"));
        project.maker().setJuniter( junitEnhancer.apply( project.maker().getJuniter()) );
    }
    
}

package org.jerkar.plugins.jacoco;

import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.tool.JkBuild;
import org.jerkar.tool.JkBuildPlugin2;
import org.jerkar.tool.builtins.javabuild.JkJavaProjectBuild;

/**
 * Alter the unitTester to be launched with the Jacoco agent. It results in
 * producing a jacoco.exec test coverage report file.
 *
 * @author Jerome Angibaud
 */
public class JkBuildPlugin2Jacoco implements JkBuildPlugin2 {

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

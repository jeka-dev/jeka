package org.jerkar.plugins.jacoco;

import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.api.project.java.JkJavaProjectPlugin;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.builtins.javabuild.JkJavaBuildPlugin;

import java.util.function.Consumer;

/**
 * Created by angibaudj on 31-08-17.
 */
public class JkJavaProjectPluginJacoco implements JkJavaProjectPlugin {

    @Override
    public void accept(JkJavaProject project) {
        JkocoJunitEnhancer junitEnhancer = JkocoJunitEnhancer.of(project.getOutLayout()
                .outputFile("jacoco/jacoco.exec"));
        project.getMakeContext().chainJuniterConfigurer((jkUnit -> junitEnhancer.enhance(jkUnit)));

    }
}

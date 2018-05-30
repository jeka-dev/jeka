package org.jerkar.plugins.jacoco;

import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.tool.JkBuild;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.JkPlugin2;
import org.jerkar.tool.builtins.java.JkPluginJava;

@JkDoc("Modify JkJavaProject in order it runs unit tests with Jacoco agent coverage test tool. " +
        "It results is production of a coverage report file.")
public class JkPluginJacoco extends JkPlugin2 {

    /**
     * Relative location to the output folder of the generated jacoco report file
     */
    public static final String OUTPUT_RELATIVE_PATH = "jacoco/jacoco.exec";

    protected JkPluginJacoco(JkBuild build) {
        super(build);
    }

    @Override
    protected void postConfigure() {
        JkPluginJava pluginJava = build.plugins.get(JkPluginJava.class);
        final JkJavaProject project = pluginJava.project();
        final JkocoJunitEnhancer junitEnhancer = JkocoJunitEnhancer.of(project.getOutLayout()
              .outputPath(OUTPUT_RELATIVE_PATH));
        project.maker().setJuniter( junitEnhancer.apply( project.maker().getJuniter()) );
    }
    
}

package org.jerkar.tool.builtins.jacoco;

import org.jerkar.api.java.junit.JkUnit;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.tool.JkBuild;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.JkDocPluginDeps;
import org.jerkar.tool.JkPlugin;
import org.jerkar.tool.builtins.java.JkPluginJava;

@JkDoc("Run unit tests with Jacoco agent coverage test tool.")
@JkDocPluginDeps(JkPluginJava.class)
public class JkPluginJacoco extends JkPlugin {

    /**
     * Relative location to the output folder of the generated jacoco report file
     */
    public static final String OUTPUT_RELATIVE_PATH = "jacoco/jacoco.exec";

    protected JkPluginJacoco(JkBuild build) {
        super(build);
    }

    @JkDoc("Configures java plugin in order unit tests are run with Jacoco coverage tool. Result is located in [OUTPUT DIR]/"
            + OUTPUT_RELATIVE_PATH + " file.")
    @Override
    protected void activate() {
        JkPluginJava pluginJava = build.plugins().get(JkPluginJava.class);
        final JkJavaProject project = pluginJava.project();
        final JkocoJunitEnhancer junitEnhancer = JkocoJunitEnhancer.of(project.maker().getOutLayout()
              .outputPath(OUTPUT_RELATIVE_PATH));
        project.maker().setTestRunner( junitEnhancer.apply((JkUnit) project.maker().getTestRunner()) );
    }
    
}

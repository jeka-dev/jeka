package org.jerkar.tool.builtins.jacoco;

import org.jerkar.api.java.junit.JkUnit;
import org.jerkar.api.java.project.JkJavaProject;
import org.jerkar.tool.JkRun;
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

    protected JkPluginJacoco(JkRun run) {
        super(run);
    }

    @JkDoc("Configures java plugin in order unit tests are run with Jacoco coverage tool. Result is located in [OUTPUT DIR]/"
            + OUTPUT_RELATIVE_PATH + " file.")
    @Override
    protected void activate() {
        JkPluginJava pluginJava = getOwner().getPlugins().get(JkPluginJava.class);
        final JkJavaProject project = pluginJava.project();
        final JkocoJunitEnhancer junitEnhancer = JkocoJunitEnhancer.of(project.getMaker().getOutLayout()
              .getOutputPath(OUTPUT_RELATIVE_PATH));
        project.getMaker().getTestTasks().setRunner( junitEnhancer.apply((JkUnit) project.getMaker().getTestTasks().getRunner()) );
    }
    
}

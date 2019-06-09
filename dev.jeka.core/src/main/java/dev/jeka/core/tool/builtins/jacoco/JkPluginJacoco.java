package dev.jeka.core.tool.builtins.jacoco;

import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkDocPluginDeps;
import dev.jeka.core.tool.JkPlugin;
import dev.jeka.core.tool.JkRun;
import dev.jeka.core.tool.builtins.java.JkPluginJava;
import dev.jeka.core.api.java.junit.JkUnit;
import dev.jeka.core.api.java.project.JkJavaProject;

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
        JkPluginJava pluginJava = getRun().getPlugins().get(JkPluginJava.class);
        final JkJavaProject project = pluginJava.getProject();
        final JkocoJunitEnhancer junitEnhancer = JkocoJunitEnhancer.of(project.getMaker().getOutLayout()
              .getOutputPath(OUTPUT_RELATIVE_PATH));
        project.getMaker().getTasksForTesting().setRunner( junitEnhancer.apply((JkUnit) project.getMaker().getTasksForTesting().getRunner()) );
    }
    
}

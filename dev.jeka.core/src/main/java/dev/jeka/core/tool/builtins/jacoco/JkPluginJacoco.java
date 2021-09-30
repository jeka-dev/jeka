package dev.jeka.core.tool.builtins.jacoco;

import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.tool.JkClass;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkDocPluginDeps;
import dev.jeka.core.tool.JkPlugin;
import dev.jeka.core.tool.builtins.java.JkPluginJava;

import java.nio.file.Path;

@JkDoc("Run unit tests with Jacoco agent coverage test tool.")
@JkDocPluginDeps(JkPluginJava.class)
public class JkPluginJacoco extends JkPlugin {

    /**
     * Relative location to the output folder of the generated jacoco report file
     */
    public static final String OUTPUT_RELATIVE_PATH = "jacoco/jacoco.exec";

    public static final String OUTPUT_XML_RELATIVE_PATH = "jacoco/jacoco.xml";

    protected JkPluginJacoco(JkClass run) {
        super(run);
    }

    @JkDoc("Configures java plugin in order unit tests are run with Jacoco coverage tool. Result is located in [OUTPUT DIR]/"
            + OUTPUT_RELATIVE_PATH + " file.")
    @Override
    protected void afterSetup() {
        JkPluginJava pluginJava = getJkClass().getPlugins().get(JkPluginJava.class);
        final JkJavaProject project = pluginJava.getProject();
        final JkocoJunitEnhancer junitEnhancer = JkocoJunitEnhancer
                .of(project.getOutputDir().resolve(OUTPUT_RELATIVE_PATH))
                .setClassDir(project.getConstruction().getCompilation().getLayout().getClassDirPath())
                .addReportOptions("--xml", project.getOutputDir().resolve(OUTPUT_XML_RELATIVE_PATH).toString());
        junitEnhancer.apply(project.getConstruction().getTesting().getTestProcessor());
    }
    
}

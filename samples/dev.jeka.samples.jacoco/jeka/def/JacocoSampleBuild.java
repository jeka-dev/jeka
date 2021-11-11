import dev.jeka.core.api.depmanagement.JkPopularModules;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.tool.JkClass;
import dev.jeka.core.tool.JkDefClasspath;
import dev.jeka.core.tool.JkPlugin;
import dev.jeka.core.tool.builtins.ide.JkPluginIntellij;
import dev.jeka.core.tool.builtins.project.JkPluginProject;
import dev.jeka.plugins.jacoco.JkPluginJacoco;

import java.nio.file.Files;
import java.nio.file.Path;

@JkDefClasspath("../../plugins/dev.jeka.plugins.jacoco/jeka/output/dev.jeka.jacoco-plugin.jar")  // For local testing
public class JacocoSampleBuild extends JkClass {

    final JkPluginProject projectPlugin = getPlugin(JkPluginProject.class);

    final JkPluginJacoco jacoco = getPlugin(JkPluginJacoco.class);

    @Override
    protected void setup() {
        jacoco.enabled = true;
        jacoco.xmlReport = true;
        jacoco.jacocoVersion = "0.8.7";
        projectPlugin.getProject().simpleFacade().setTestDependencies(deps -> deps
                        .and(JkPopularModules.JUNIT_5 + ":5.8.1")
        );
    }

    // For local testing
    public void checkReportGenerated() {
        Path report = getOutputDir().resolve("jacoco/jacoco.xml");
        JkUtilsAssert.state(Files.exists(report), "Report file " + report + " not found.");
    }


}
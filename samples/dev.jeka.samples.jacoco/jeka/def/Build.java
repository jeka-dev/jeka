import dev.jeka.core.api.depmanagement.JkPopularModules;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.tool.*;
import dev.jeka.core.tool.builtins.java.JkPluginJava;
import dev.jeka.plugins.jacoco.JkPluginJacoco;

import java.nio.file.Files;
import java.nio.file.Path;

@JkDefClasspath("../../plugins/dev.jeka.plugins.jacoco/jeka/output/dev.jeka.jacoco-plugin.jar")  // For local testing
public class Build extends JkClass {

    final JkPluginJava java = getPlugin(JkPluginJava.class);

    final JkPluginJacoco jacoco = getPlugin(JkPluginJacoco.class);

    @Override
    protected void setup() {
        jacoco.enabled = true;
        jacoco.xmlReport = true;
        jacoco.jacocoVersion = "0.8.7";
        java.getProject().simpleFacade().setTestDependencies(deps -> deps
                        .and(JkPopularModules.JUNIT_5 + ":5.8.1")
        );
    }

    public void checkReportGenerated() {
        Path report = getOutputDir().resolve("jacoco/jacoco.xml");
        JkUtilsAssert.state(Files.exists(report), "Report file " + report + " not found.");
    }
}
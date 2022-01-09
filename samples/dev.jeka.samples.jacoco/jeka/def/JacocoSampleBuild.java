import dev.jeka.core.api.depmanagement.JkPopularModules;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.tooling.intellij.JkIml;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkInjectClasspath;
import dev.jeka.core.tool.builtins.ide.IntellijJkBean;
import dev.jeka.core.tool.builtins.project.ProjectJkBean;
import dev.jeka.plugins.jacoco.JacocoJkBean;

import java.nio.file.Files;
import java.nio.file.Path;

@JkInjectClasspath("../../plugins/dev.jeka.plugins.jacoco/jeka/output/dev.jeka.jacoco-plugin.jar")  // For local testing
public class JacocoSampleBuild extends JkBean {

    ProjectJkBean projectPlugin = getBean(ProjectJkBean.class).configure(this::configure);

    JacocoJkBean jacoco = getRuntime().getBean(JacocoJkBean.class);

    IntellijJkBean intellij = getRuntime().getBean(IntellijJkBean.class);

    protected JacocoSampleBuild() {
        jacoco.enabled = true;
        jacoco.xmlReport = true;
        jacoco.jacocoVersion = "0.8.7";
        intellij.configureImlGenerator(imlGenerator -> imlGenerator.setSkipJeka(true));
        intellij.configureIml(this::configureIml);
    }

    private void configure(JkProject project) {
        project.simpleFacade()
                .configureTestDeps(deps -> deps
                        .and(JkPopularModules.JUNIT_5 + ":5.8.1")
                );
    }

    // For local testing
    public void checkReportGenerated() {
        Path report = getOutputDir().resolve("jacoco/jacoco.xml");
        JkUtilsAssert.state(Files.exists(report), "Report file " + report + " not found.");
    }

    private void configureIml(JkIml iml) {
       iml.getComponent()
               .replaceLibByModule("dev.jeka.jacoco-plugin.jar", "dev.jeka.plugins.jacoco");
    }


}
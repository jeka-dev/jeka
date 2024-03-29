import dev.jeka.core.api.depmanagement.JkPopularLibs;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.intellij.JkIml;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.JkInjectClasspath;
import dev.jeka.core.tool.builtins.ide.IntellijJkBean;
import dev.jeka.core.tool.builtins.project.ProjectJkBean;
import dev.jeka.plugins.jacoco.JacocoJkBean;

import java.nio.file.Files;
import java.nio.file.Path;

@JkInjectClasspath("../../plugins/dev.jeka.plugins.jacoco/jeka/output/dev.jeka.jacoco-plugin.jar")  // For local testing
public class JacocoSampleBuild extends JkBean {

    ProjectJkBean projectKBean = getBean(ProjectJkBean.class).lately(this::configure);

    JacocoJkBean jacocoKBean = getRuntime().getBean(JacocoJkBean.class);

    final IntellijJkBean intellijKBean = getBean(IntellijJkBean.class);

    protected JacocoSampleBuild() {
        jacocoKBean.jacocoVersion = "0.8.7";
        intellijKBean.configureImlGenerator(imlGenerator -> imlGenerator.setExcludeJekaLib(true));
        intellijKBean.configureIml(this::configureIml);
    }

    private void configure(JkProject project) {
        project.flatFacade()
                .configureTestDependencies(deps -> deps
                        .and(JkPopularLibs.JUNIT_5 + ":5.8.2")
                );
    }

    // For local testing
    public void checkReportGenerated() {
        Path report = getOutputDir().resolve("jacoco/jacoco.xml");
        JkUtilsAssert.state(Files.exists(report), "Report file " + report + " not found.");
    }

    private void configureIml(JkIml iml) {
       iml.component
               .replaceLibByModule("dev.jeka.jacoco-plugin.jar", "dev.jeka.plugins.jacoco");
        iml.component.replaceLibByModule("dev.jeka.jeka-core.jar", "dev.jeka.core");
    }

    public static void main(String[] args) {
        JacocoSampleBuild build = JkInit.instanceOf(JacocoSampleBuild.class);
        JkLog.setVerbosity(JkLog.Verbosity.VERBOSE);
        build.cleanOutput();
        build.projectKBean.test();
    }


}
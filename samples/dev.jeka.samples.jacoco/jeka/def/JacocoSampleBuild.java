import dev.jeka.core.api.depmanagement.JkPopularLibs;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.JkInjectClasspath;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.ide.IntellijKBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.plugins.jacoco.JacocoKBean;

import java.nio.file.Files;
import java.nio.file.Path;

@JkInjectClasspath("../../plugins/dev.jeka.plugins.jacoco/jeka/output/dev.jeka.jacoco-plugin.jar")  // For local testing
public class JacocoSampleBuild extends KBean {

    ProjectKBean projectKBean = load(ProjectKBean.class);

    JacocoKBean jacocoKBean = load(JacocoKBean.class);

    IntellijKBean intelliKBean = load(IntellijKBean.class)
            .replaceLibByModule("dev.jeka.jacoco-plugin.jar", "dev.jeka.plugins.jacoco")
            .replaceLibByModule("dev.jeka.jeka-core.jar", "dev.jeka.core");

    protected JacocoSampleBuild() {
        jacocoKBean.jacocoVersion = "0.8.7";
    }

    @Override
    protected void init() {
        projectKBean.project.flatFacade()
                .configureTestDependencies(deps -> deps
                        .and(JkPopularLibs.JUNIT_5 + ":5.8.2")
                );
    }

    // For local testing
    public void checkReportGenerated() {
        Path report = getOutputDir().resolve("jacoco/jacoco.xml");
        JkUtilsAssert.state(Files.exists(report), "Report file " + report + " not found.");
    }

    public static void main(String[] args) {
        JacocoSampleBuild build = JkInit.instanceOf(JacocoSampleBuild.class);
        JkLog.setVerbosity(JkLog.Verbosity.VERBOSE);
        build.cleanOutput();
        build.projectKBean.test();
    }


}
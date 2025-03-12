import dev.jeka.core.api.depmanagement.JkPopularLibs;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.tool.*;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.tooling.ide.IntellijKBean;
import dev.jeka.plugins.jacoco.JkJacoco;

import java.nio.file.Files;
import java.nio.file.Path;

@JkDep("../../plugins/plugins.jacoco/jeka-output/dev.jeka.jacoco-plugin.jar")  // For local testing
public class JacocoSampleCustom extends KBean {

    @JkInject
    ProjectKBean projectKBean;

    @JkPostInit
    private void postInit(IntellijKBean intellijKBean) {
        intellijKBean.replaceLibByModule("dev.jeka.jacoco-plugin.jar", "plugins.jacoco")
                .replaceLibByModule("dev.jeka.jeka-core.jar", "core");
    }

    @JkPostInit
    private void postInit(ProjectKBean projectKBean) {
        JkProject project = projectKBean.project;
        JkJacoco.ofVersion("0.8.11")
                .configureAndApplyTo(project);
        project.flatFacade.dependencies.test
                .add(JkPopularLibs.JUNIT_5 + ":5.8.2");
    }

    // For local testing
    public void checkGeneratedReport() {
        Path report = getOutputDir().resolve("jacoco/jacoco.xml");
        JkUtilsAssert.state(Files.exists(report), "Report file " + report + " not found.");
    }

    public static void main(String[] args) {
        JacocoSampleCustom build = JkInit.kbean(JacocoSampleCustom.class);
        JkLog.setVerbosity(JkLog.Verbosity.VERBOSE);
        build.cleanOutput();
        build.projectKBean.test();
    }

}
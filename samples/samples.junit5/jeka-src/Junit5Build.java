import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.testing.JkTestProcessor;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.tool.*;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.tooling.ide.IntellijKBean;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * This build class illustrates how to use directly JUnit5 Platform API.
 * <p>
 * To use JUnit5 Platform API in the build class, you need to declare *platform-launcher* library on
 * the def classpath as done below.
 * <p>
 * Normally, for most of the cases, you won't need it has the JeKa API are yet powerful enough to customize
 * most of the testing scenario.
 * <p>
 * There is 2 points where you can add *junit5 native* instructions from a {@link JkTestProcessor} :
 * <ul>
 *     <li>getEngineBehavior().setLauncherConfigurer()</li>
 *     <li>getTestSelection().setDiscoveryConfigurer()</li>
 * </ul>
 * From this two points you can customize the builder to be used for creating the actual
 * {@link org.junit.platform.launcher.core.LauncherConfig} and
 * {@link org.junit.platform.launcher.LauncherDiscoveryRequest}.
 */
@JkDep("org.junit.platform:junit-platform-launcher:1.8.2")
class Junit5Build extends KBean {

    @JkInject
    private ProjectKBean projectKBean;

    @JkPostInit
    protected void postInit(IntellijKBean intellijKBean) {
        intellijKBean
                .replaceLibByModule("dev.jeka.jeka-core.jar", "core");
    }

    @JkPostInit
    protected void postInit(ProjectKBean projectKBean) {
        JkProject project = projectKBean.project;
        project
            .testing
                .compilation
                    .dependencies
                        .add("org.jdom:jdom2:2.0.6")
                        .add("org.junit.jupiter:junit-jupiter:5.8.2");
        project
            .testing
                .testProcessor
                    .engineBehavior
                        .setLauncherConfigurer(builder -> builder  // Junit5-platform API. see nit.org/junit5/docs/5.3.0/api/org/junit/platform/launcher/core/LauncherConfig.html
                            .addTestExecutionListeners(new MyJunit5PlatformListener()));
        project
            .testing
                .testSelection
                    .setDiscoveryConfigurer(builder -> builder  // see https://junit.org/junit5/docs/5.0.0/api/org/junit/platform/launcher/core/LauncherDiscoveryRequestBuilder.html
                        .configurationParameter("key1", "value1")
                        .selectors(
                            DiscoverySelectors.selectMethod("dev.jeka.core.samples.FooTest#testDisplay")));
    }

    static class MyJunit5PlatformListener implements TestExecutionListener {

        @Override
        public void testPlanExecutionStarted(TestPlan testPlan) {
            System.out.println("Test plan " + testPlan + " is being executed ...");
        }

        @Override
        public void testPlanExecutionFinished(TestPlan testPlan) {
            System.out.println("Test plan " + testPlan + " has been executed ...");
        }
    }

    public void cleanPack() {
        cleanOutput(); projectKBean.pack();
    }

    public void checkReportGenerated() {
        Path report = getOutputDir().resolve("test-report");
        JkUtilsAssert.state(Files.exists(report), "Report file " + report + " not found.");
    }

    public static void main(String[] args) {
        JkInit.kbean(Junit5Build.class, args).cleanPack();
    }

}
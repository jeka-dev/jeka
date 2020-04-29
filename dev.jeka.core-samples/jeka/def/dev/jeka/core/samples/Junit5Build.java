package dev.jeka.core.samples;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.tool.JkCommandSet;
import dev.jeka.core.tool.JkDefClasspath;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.builtins.java.JkPluginJava;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

import static dev.jeka.core.api.depmanagement.JkScope.TEST;

/**
 * This build class illustrates how to use directly JUnit5 Platform API.
 *
 * To use JUnit5 Platform API in the build class, you need to declare *plateform-launcher* library on
 * the def classpath as done below.
 *
 * Normally, for most of cases, you won't need it has the Jeka API are yet powerful enough to customize
 * most of the testing scenario.
 *
 * There is 2 points where you can add *junit5 native* instructions from a {@link dev.jeka.core.api.java.testing.JkTestProcessor} :
 * <ul>
 *     <li>getEngineBehavior().setLauncherConfigurer()</li>
 *     <li>getTestSelection().setDiscoveryConfigurer()</li>
 * </ul>
 * From this two points you can customize the builder to be used for creating the actual
 * {@link org.junit.platform.launcher.core.LauncherConfig} and
 * {@link org.junit.platform.launcher.LauncherDiscoveryRequest}.
 */
@JkDefClasspath("org.junit.platform:junit-platform-launcher:1.6.0")
class Junit5Build extends JkCommandSet {

    final JkPluginJava java = getPlugin(JkPluginJava.class);

    /*
     * Configures plugins to be bound to this command class. When this method is called, option
     * fields have already been injected from command line.
     */
    @Override
    protected void setup() {
        java.getProject()
            .getDependencyManagement()
                .addDependencies(JkDependencySet.of()
                    .and("org.junit.jupiter:junit-jupiter-engine:5.1.0", TEST)
                    .and("org.junit.vintage:junit-vintage-engine:jar:5.6.0", TEST)
                    .and("org.jdom:jdom2:2.0.6", TEST)
                ).__
            .getTesting()
                .getTestProcessor()
                    .getEngineBehavior()
                        .setLauncherConfigurer(builder -> builder  // Junit5-platform API. see nit.org/junit5/docs/5.3.0/api/org/junit/platform/launcher/core/LauncherConfig.html
                            .addTestExecutionListeners(new MyJunit5PlatformListener())).__.__
                .getTestSelection()
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
        clean(); java.pack();
    }

    public static void main(String[] args) {
        JkInit.instanceOf(Junit5Build.class).cleanPack();
    }

}
import dev.jeka.core.api.depmanagement.JkPopularModules;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.tooling.JkGitProcess;
import dev.jeka.core.tool.JkClass;
import dev.jeka.core.tool.JkDefClasspath;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.builtins.java.JkPluginJava;
import dev.jeka.plugins.sonarqube.JkPluginSonarqube;
import dev.jeka.plugins.sonarqube.JkSonarqube;

/**
 * As there we have no embedded sonar server, this sample cannot be run automatically.
 * User has to run or use an existing server.
 * By default, sonar
 */
@JkDefClasspath("../../plugins/dev.jeka.plugins.sonarqube/jeka/output/dev.jeka.sonarqube-plugin.jar")  // for local test
class SonarqubeSampleBuild extends JkClass {

    private final JkPluginJava javaPlugin = getPlugin(JkPluginJava.class);

    private final JkPluginSonarqube sonarqubePlugin = getPlugin(JkPluginSonarqube.class);

    @Override
    protected void setup() {
        javaPlugin.getProject().simpleFacade()
            .setJvmTargetVersion(JkJavaVersion.V8)
            .setCompileDependencies(deps -> deps
                .and("com.github.djeang:vincer-dom:1.4.0")
            )
            .setTestDependencies(deps -> deps
                .and(JkPopularModules.JUNIT_5 + ":+")
            );
        sonarqubePlugin.provideTestLibs = true;
        sonarqubePlugin.configure(sonarqube -> {
            sonarqube
                .setProjectId("dev.jeka.samples.sonarqube", "myProjectNme",
                        JkGitProcess.of().getVersionFromTag())
                .setProperty(JkSonarqube.HOST_URL, "https://my.host.for.sonarqube.server:8080")
                .setSkipDesign(true);
        });
    }

    public void cleanPack() {
        clean(); javaPlugin.pack();
    }

    @JkDoc("Cleans, tests and creates bootable jar.")
    public void cleanSonar() {
        clean();
        javaPlugin.test();
        sonarqubePlugin.run();
    }

    // Clean, compile, test and generate springboot application jar
    public static void main(String[] args) {
        JkInit.instanceOf(SonarqubeSampleBuild.class, args).cleanPack();
    }

}
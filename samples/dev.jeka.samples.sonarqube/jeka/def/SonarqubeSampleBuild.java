import dev.jeka.core.api.depmanagement.JkPopularModules;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.tooling.JkGitProcess;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkInjectClasspath;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.builtins.project.ProjectJkBean;
import dev.jeka.plugins.sonarqube.JkPluginSonarqube;
import dev.jeka.plugins.sonarqube.JkSonarqube;

/**
 * As there we have no embedded sonar server, this sample cannot be run automatically.
 * User has to run or use an existing server.
 * By default, sonar
 */
@JkInjectClasspath("../../plugins/dev.jeka.plugins.sonarqube/jeka/output/dev.jeka.sonarqube-plugin.jar")  // for local test
class SonarqubeSampleBuild extends JkBean {

    private final ProjectJkBean projectPlugin = getRuntime().getBean(ProjectJkBean.class);

    private final JkPluginSonarqube sonarqubePlugin = getRuntime().getBean(JkPluginSonarqube.class);

    @Override
    protected void init() {
        projectPlugin.getProject().simpleFacade()
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
        clean(); projectPlugin.pack();
    }

    @JkDoc("Cleans, tests and creates bootable jar.")
    public void cleanSonar() {
        clean();
        projectPlugin.test();
        sonarqubePlugin.run();
    }

    // Clean, compile, test and generate springboot application jar
    public static void main(String[] args) {
        JkInit.instanceOf(SonarqubeSampleBuild.class, args).cleanPack();
    }

}
import dev.jeka.core.api.depmanagement.JkPopularLibs;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.tooling.JkGit;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.JkInjectClasspath;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.ide.IntellijKBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.plugins.sonarqube.JkSonarqube;
import dev.jeka.plugins.sonarqube.SonarqubeKBean;

/**
 * As there we have no embedded sonar server, this sample cannot be run automatically.
 * User has to run or use an existing server.
 * By default, sonar
 */
@JkInjectClasspath("../../plugins/dev.jeka.plugins.sonarqube/jeka/output/dev.jeka.sonarqube-plugin.jar")  // for local test
class SonarqubeSampleBuild extends KBean {

    IntellijKBean intelliKBean = load(IntellijKBean.class)
            .replaceLibByModule("dev.jeka.sonarqubbe-plugin.jar", "dev.jeka.plugins.sonarqube")
            .replaceLibByModule("dev.jeka.jeka-core.jar", "dev.jeka.core");

    private final ProjectKBean projectKBean = load(ProjectKBean.class);

    private final SonarqubeKBean sonarqubeKBean = load(SonarqubeKBean.class);

    @Override
    protected void init() {
        projectKBean.project.flatFacade()
                .setJvmTargetVersion(JkJavaVersion.V8)
                .configureCompileDependencies(deps -> deps
                    .and("com.github.djeang:vincer-dom:1.4.0")
                )
                .configureTestDependencies(deps -> deps
                    .and(JkPopularLibs.JUNIT_5 + ":5.8.2")
                );
        sonarqubeKBean.provideTestLibs = true;
        sonarqubeKBean.sonarqube
                .setProjectId("dev.jeka.samples.sonarqube", "myProjectNme",
                        JkGit.of().getVersionFromTag())
                .setProperty(JkSonarqube.HOST_URL, "https://my.host.for.sonarqube.server:8080")
                .setSkipDesign(true);

        // Use intellij module dependency instead of ja dependency
        load(IntellijKBean.class)
                .replaceLibByModule("dev.jeka.jeka-core.jar", "dev.jeka.core");
    }

    @JkDoc("Cleans, tests and creates bootable jar.")
    public void cleanSonar() {
        cleanOutput();
        projectKBean.test();
        sonarqubeKBean.run();
    }

    public void cleanPack() {
        cleanOutput(); projectKBean.pack();
    }

    // Clean, compile, test and generate springboot application jar
    public static void main(String[] args) {
        JkInit.instanceOf(SonarqubeSampleBuild.class, args).cleanPack();
    }

}
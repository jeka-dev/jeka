import dev.jeka.core.api.depmanagement.JkPopularLibs;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.tooling.git.JkGit;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.JkInjectClasspath;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.tooling.ide.IntellijKBean;
import dev.jeka.plugins.sonarqube.JkSonarqube;

/**
 * As there we have no embedded sonar server, this sample cannot be run automatically.
 * User has to run or use an existing server.
 * By default, sonar
 */
@JkInjectClasspath("../../plugins/dev.jeka.plugins.sonarqube/jeka/output/dev.jeka.sonarqube-plugin.jar")  // for local test
class SonarqubeSampleBuild extends KBean {

    SonarqubeSampleBuild() {

        // Use intellij source dependencies to ease plugin development.
        load(IntellijKBean.class)
                .replaceLibByModule("dev.jeka.sonarqubbe-plugin.jar", "dev.jeka.plugins.sonarqube")
                .replaceLibByModule("dev.jeka.jeka-core.jar", "dev.jeka.core");
    }

    private final ProjectKBean projectKBean = load(ProjectKBean.class);

    private JkSonarqube sonarqube;

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
        sonarqube = JkSonarqube.ofEmbedded()
                .setProjectId("dev.jeka.samples.sonarqube", "myProjectNme",
                        JkGit.of().getVersionFromTag())
                .setProperty(JkSonarqube.HOST_URL, "https://my.host.for.sonarqube.server:8080")
                .setSkipDesign(true)
                .configureFor(projectKBean.project);
    }

    @JkDoc("Cleans, tests and creates bootable jar.")
    public void sonar() {
        cleanOutput();
        projectKBean.test();
        sonarqube.run();
    }

    public void cleanPack() {
        cleanOutput(); projectKBean.pack();
    }

    // Clean, compile, test and generate springboot application jar
    public static void main(String[] args) {
        JkInit.instanceOf(SonarqubeSampleBuild.class, args).cleanPack();
    }

}
import dev.jeka.core.CoreBuild;
import dev.jeka.core.api.crypto.gpg.JkGpg;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.publication.JkNexusRepos;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.tooling.JkGitProcess;
import dev.jeka.core.tool.*;
import dev.jeka.core.tool.builtins.git.GitJkBean;
import dev.jeka.core.tool.builtins.project.ProjectJkBean;
import dev.jeka.core.tool.builtins.repos.NexusJkBean;
import dev.jeka.plugins.jacoco.JacocoJkBean;
import dev.jeka.plugins.sonarqube.SonarqubeJkBean;
import github.Github;

import java.io.IOException;

class MasterBuild extends JkBean {

    @JkInjectProperty("OSSRH_USER")
    public String ossrhUser;

    @JkInjectProperty("OSSRH_PWD")
    public String ossrhPwd;

    @JkInjectProperty("GITHUB_TOKEN")
    public String githubToken;

    public boolean runSamples = true;

    public boolean useJacoco = false;

    final NexusJkBean nexus = getBean(NexusJkBean.class).lately(this::configure);

    final GitJkBean git = getBean(GitJkBean.class);


    // ------ Slave projects

    @JkInjectProject("../dev.jeka.core")
    CoreBuild coreBuild;

    @JkInjectProject("../plugins/dev.jeka.plugins.jacoco")
    JacocoBuild jacocoBuild;

    @JkInjectProject("../plugins/dev.jeka.plugins.sonarqube")
    SonarqubeBuild sonarqubeBuild;

    @JkInjectProject("../plugins/dev.jeka.plugins.springboot")
    SpringbootBuild springbootBuild;

    @JkInjectProject("../plugins/dev.jeka.plugins.nodejs")
    NodeJsBuild nodeJsBuild;

    @JkInjectProject("../plugins/dev.jeka.plugins.kotlin")
    KotlinBuild kotlinBuild;

    @JkInjectProject("../plugins/dev.jeka.plugins.protobuf")
    ProtobufBuild protobufBuild;

    private JacocoJkBean coreJacocoBean;

    MasterBuild()  {
        git.configureProjectVersion = false;
        coreBuild.runIT = true;
        getImportedBeans().get(ProjectJkBean.class, false).forEach(this::applyToSlave);
        if (getRuntime().getProperties().get("sonar.host.url") != null) {
            useJacoco = true;
        }
        if (useJacoco) {
            coreJacocoBean = coreBuild.getBean(JacocoJkBean.class).setHtmlReport(true);
        }
    }

    @JkDoc("Clean build of core and plugins + running all tests + publish if needed.")
    public void make() throws IOException {
        JkLog.startTask("Building core and plugins");
        getImportedBeans().get(ProjectJkBean.class, false).forEach(bean -> {
            JkLog.startTask("Running KBean " + bean);
            bean.clean();
            bean.pack();
            JkLog.endTask();
        });
        JkLog.endTask();
        if (runSamples) {
            JkLog.startTask("Running samples");
            SamplesTester samplesTester = new SamplesTester(this.getRuntime().getProperties());
            PluginScaffoldTester pluginScaffoldTester = new PluginScaffoldTester(this.getRuntime().getProperties());
            if (coreJacocoBean != null) {
                JacocoJkBean.AgentJarAndReportFile jacocoRunInfo = coreJacocoBean.getAgentAndReportFile();
                samplesTester.setJacoco(jacocoRunInfo.getAgentPath(), jacocoRunInfo.getReportFile());
                pluginScaffoldTester.setJacoco(jacocoRunInfo.getAgentPath(), jacocoRunInfo.getReportFile());
            }
            samplesTester.run();
            pluginScaffoldTester.run();
            if (coreJacocoBean != null) {
                coreJacocoBean.generateExport();
            }
            JkLog.endTask();
        }
        String branch = JkGitProcess.of().getCurrentBranch();
        if (branch.equals("master") && ossrhUser != null) {
            JkLog.startTask("Publishing artifacts to Maven Central");
            getImportedBeans().get(ProjectJkBean.class, false).forEach(ProjectJkBean::publish);
            closeAndReleaseRepo();
            JkLog.endTask();
            JkLog.startTask("Creating GitHub Release");
            Github github = new Github();
            github.ghToken =githubToken;
            github.publishGhRelease();
            JkLog.endTask();;

        }
        if (getRuntime().getProperties().get("sonar.host.url") != null) {
            coreBuild.getBean(SonarqubeJkBean.class).run();
        }
        publishLocal();;
    }

    @JkDoc("Convenient method to set Posix permission for all jekaw files on git.")
    public void setPosixPermissions() {
        JkPathTree.of("../samples").andMatching("*/jekaw", "**/jekaw").getFiles().forEach(path -> {
            JkLog.info("Setting exec permission on git for file " + path);
            JkProcess.ofCmdLine("git update-index --chmod=+x " + path).run();
        });
    }

    @JkDoc("Closes and releases staging Nexus repositories (typically, after a publish).")
    public void closeAndReleaseRepo() {
        JkRepo repo = publishRepo().getRepoConfigHavingUrl(JkRepo.MAVEN_OSSRH_DEPLOY_RELEASE);
        JkNexusRepos.ofRepo(repo).closeAndRelease();
    }

    @JkDoc("Clean build of core + plugins bypassing tests.")
    public void buildFast() {
        getImportedBeans().get(ProjectJkBean.class, false).forEach(bean -> {
            bean.getProject().flatFacade().skipTests(true);
            bean.getProject().includeJavadocAndSources(false, false);
            bean.clean();
            bean.getProject().pack();
        });
    }

    private void configure(JkNexusRepos nexusRepos) {
        nexusRepos.setReadTimeout(60*1000);
    }

    private JkRepoSet publishRepo() {
        JkRepo snapshotRepo = JkRepo.ofMavenOssrhDownloadAndDeploySnapshot(ossrhUser, ossrhPwd);
        JkGpg gpg = JkGpg.ofStandardProject(this.getBaseDir());
        JkRepo releaseRepo =  JkRepo.ofMavenOssrhDeployRelease(ossrhUser, ossrhPwd,  gpg.getSigner(""));
        releaseRepo.publishConfig
                    .setVersionFilter(jkVersion -> !jkVersion.isSnapshot());
        JkRepo githubRepo = JkRepo.ofGitHub("jeka-dev", "jeka");
        githubRepo.publishConfig.setVersionFilter(jkVersion -> !jkVersion.isSnapshot());
        return  JkRepoSet.of(snapshotRepo, releaseRepo, githubRepo);
    }

    private void applyToSlave(ProjectJkBean projectJkBean) {
        if (!git.version().isSnapshot()) {     // Produce javadoc only for release
            projectJkBean.pack.javadoc = true;
        }
        projectJkBean.lately(project -> {
                git.handleVersioning(project, false);
                project.publication
                    .setRepos(this.publishRepo())
                    .maven
                        .pomMetadata
                            .setProjectUrl("https://jeka.dev")
                            .setScmUrl("https://github.com/jerkar/jeka.git")
                            .addApache2License();
        });
    }

    public void buildCore() {
        coreBuild.cleanPack();
    }

    public void runSamples()  {
        new SamplesTester(this.getRuntime().getProperties()).run();
    }

    public void runScaffoldsWithPlugins() {
        new PluginScaffoldTester(this.getRuntime().getProperties()).run();
    }

    public void publishLocal() {
        getImportedBeans().get(ProjectJkBean.class, false).forEach(ProjectJkBean::publishLocal);
    }

    public static void main(String[] args) throws Exception {
        JkInit.instanceOf(MasterBuild.class, args).make();
    }

    static class BuildFast {
        public static void main(String[] args) {
            JkInit.instanceOf(MasterBuild.class, args).buildFast();
        }
    }

    static class ShowVersion {
        public static void main(String[] args) {
            System.out.println(JkInit.instanceOf(GitJkBean.class, args).version());
        }
    }


}

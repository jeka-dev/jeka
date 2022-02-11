import dev.jeka.core.CoreBuild;
import dev.jeka.core.api.crypto.gpg.JkGpg;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.publication.JkNexusRepos;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.system.JkProperty;
import dev.jeka.core.api.tooling.JkGitProcess;
import dev.jeka.core.tool.*;
import dev.jeka.core.tool.builtins.project.ProjectJkBean;
import dev.jeka.core.tool.builtins.release.VersionFromGitJkBean;
import dev.jeka.core.tool.builtins.repos.NexusJkBean;
import dev.jeka.plugins.jacoco.JacocoJkBean;
import dev.jeka.plugins.sonarqube.SonarqubeJkBean;

class MasterBuild extends JkBean {

    @JkInjectProperty("OSSRH_USER")
    public String ossrhUser;

    @JkInjectProperty("OSSRH_PWD")
    public String ossrhPwd;

    @JkInjectProperty("GH_TOKEN")
    public String githubToken;

    public boolean runSamples = true;

    public boolean useJacoco = false;

    final NexusJkBean nexus = getBean(NexusJkBean.class).configure(this::configure);

    final VersionFromGitJkBean versionFromGit = getBean(VersionFromGitJkBean.class);


    // ------ Slave projects

    @JkInjectProject("../dev.jeka.core")
    CoreBuild coreBuild;

    @JkInjectProject("../plugins/dev.jeka.plugins.jacoco")
    JacocoBuild jacocoBuild;

    @JkInjectProject("../plugins/dev.jeka.plugins.sonarqube")
    SonarqubeBuild sonarqubeBuild;

    @JkInjectProject("../plugins/dev.jeka.plugins.springboot")
    SpringbootBuild springbootBuild;

    private JacocoJkBean coreJacocoBean;

    MasterBuild() throws Exception {
        versionFromGit.autoConfigureProject = false;
        coreBuild.runIT = true;
        getImportedJkBeans().get(ProjectJkBean.class, false).forEach(this::applyToSlave);
        if (JkProperty.get("sonar.host.url") != null) {
            useJacoco = true;
        }
        if (useJacoco) {
            coreJacocoBean = coreBuild.getBean(JacocoJkBean.class).setHtmlReport(true);
        }
    }

    @JkDoc("Clean build of core and plugins + running all tests + publish if needed.")
    public void make() {
        JkLog.startTask("Building core and plugins");
        getImportedJkBeans().get(ProjectJkBean.class, false).forEach(bean -> {
            JkLog.startTask("Running KBean " + bean);
            bean.clean();
            bean.pack();
            JkLog.endTask();
        });
        JkLog.endTask();
        if (runSamples) {
            JkLog.startTask("Running samples");
            SamplesTester samplesTester = new SamplesTester();
            PluginScaffoldTester pluginScaffoldTester = new PluginScaffoldTester();
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
        if (branch.equals("master") && !versionFromGit.version().isSnapshot()) {
            JkLog.startTask("Publishing");
            getImportedJkBeans().get(ProjectJkBean.class, false).forEach(ProjectJkBean::publish);
            closeAndReleaseRepo();
            JkLog.endTask();
        }
        if (JkProperty.get("sonar.host.url") != null) {
            coreBuild.getBean(SonarqubeJkBean.class).run();
        }
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
        JkNexusRepos.ofUrlAndCredentials(repo).closeAndRelease();
    }

    @JkDoc("Clean build of core + plugins bypassing tests.")
    public void buildFast() {
        getImportedJkBeans().get(ProjectJkBean.class, false).forEach(bean -> {
            bean.getProject().simpleFacade().skipTests(true);
            bean.getProject().includeJavadocAndSources(false, false);
            bean.clean();
            bean.getProject().pack();
        });
    }

    private void configure(JkNexusRepos nexusRepos) {
        nexusRepos.setReadTimeout(60*1000);
    }

    private JkRepoSet publishRepo() {
        JkRepo snapshotRepo = JkRepo.ofMavenOssrhDownloadAndDeploySnapshot(ossrhPwd, ossrhPwd);
        JkGpg gpg = JkGpg.ofStandardProject(this.getBaseDir());
        JkRepo releaseRepo =  JkRepo.ofMavenOssrhDeployRelease(ossrhPwd, ossrhPwd,  gpg.getSigner(""))
                .getPublishConfig()
                    .setVersionFilter(jkVersion -> jkVersion.getValue().endsWith(".RELEASE")).__;
        JkRepo githubRepo = JkRepo.ofGitHub("jeka-dev", "jeka")
                .setCredentials("", githubToken);
        return  JkRepoSet.of(snapshotRepo, releaseRepo, githubRepo);
    }

    private void applyToSlave(ProjectJkBean projectJkBean) {
        if (!versionFromGit.version().isSnapshot()) {     // Produce javadoc only for release
            projectJkBean.pack.javadoc = true;
        }
        projectJkBean.configure(project -> {
                versionFromGit.configure(project, false);
                project.getPublication()
                    .setVersion(versionFromGit::versionAsText)
                    .setRepos(this.publishRepo())
                    .getMaven()
                        .getPomMetadata()
                            .setProjectUrl("https://jeka.dev")
                            .setScmUrl("https://github.com/jerkar/jeka.git")
                            .addApache2License();
        });
    }

    public void buildCore() {
        coreBuild.cleanPack();
    }

    public void runSamples()  {
        new SamplesTester().run();
    }

    public void runScaffoldsWithPlugins() {
        new PluginScaffoldTester().run();
    }


    public void publishLocal() {
        getImportedJkBeans().get(ProjectJkBean.class, false).forEach(ProjectJkBean::publishLocal);
    }

    public static void main(String[] args) {
        JkInit.instanceOf(MasterBuild.class, args).make();
    }

    static class BuildFast {
        public static void main(String[] args) {
            JkInit.instanceOf(MasterBuild.class, args).buildFast();
        }
    }

    static class ShowVersion {
        public static void main(String[] args) {
            System.out.println(JkInit.instanceOf(VersionFromGitJkBean.class, args).version());
        }
    }

}

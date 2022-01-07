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
import dev.jeka.core.tool.builtins.project.ProjectJkBean;
import dev.jeka.core.tool.builtins.release.VersionFromGitJkBean;
import dev.jeka.core.tool.builtins.repos.NexusJkBean;

class MasterBuild extends JkBean {

    @JkInjectProperty("OSSRH_USER")
    public String ossrhUser;

    @JkInjectProperty("OSSRH_PWD")
    public String ossrhPwd;

    @JkInjectProperty("GH_TOKEN")
    public String githubToken;

    final NexusJkBean nexus = getRuntime().getBean(NexusJkBean.class);

    final VersionFromGitJkBean versionFromGit = getRuntime().getBean(VersionFromGitJkBean.class);

    // ------ Slave projects

    @JkInjectProject("../dev.jeka.core")
    CoreBuild coreBuild;

    @JkInjectProject("../plugins/dev.jeka.plugins.jacoco")
    JacocoBuild jacocoBuild;

    @JkInjectProject("../plugins/dev.jeka.plugins.sonarqube")
    SonarqubeBuild sonarqubeBuild;

    @JkInjectProject("../plugins/dev.jeka.plugins.springboot")
    SpringbootBuild springbootBuild;

    private JkRepoSet publishRepos;


    @Override
    protected void init() throws Exception {
        versionFromGit.autoConfigureProject = false;
        coreBuild.runIT = true;
        JkGpg gpg = JkGpg.ofStandardProject(this.getBaseDir());
        publishRepos = JkRepoSet.ofOssrhSnapshotAndRelease(ossrhUser, ossrhPwd, gpg.getSigner(""));
        getImportedJkBeans().get(ProjectJkBean.class, false).forEach(this::configureSlave);
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
        JkLog.startTask("Running samples");
        runSamples();
        runScaffoldsWithPlugins();
        JkLog.endTask();
        String branch = JkGitProcess.of().getCurrentBranch();
        if (branch.equals("master") && !versionFromGit.version().isSnapshot()) {
            JkLog.startTask("Publishing");
            getImportedJkBeans().get(ProjectJkBean.class, false).forEach(ProjectJkBean::publish);
            closeAndReleaseRepo();
            JkLog.endTask();
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
        JkRepo repo = publishRepos.getRepoConfigHavingUrl(JkRepo.MAVEN_OSSRH_DEPLOY_RELEASE);
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

    private void configureSlave(ProjectJkBean projectPlugin) {
        versionFromGit.configure(projectPlugin.getProject(), false);
        if (!versionFromGit.version().isSnapshot()) {     // Produce javadoc only for release
            projectPlugin.pack.javadoc = true;
        }
        projectPlugin.getProject().getPublication()
                .setVersion(versionFromGit::versionAsText)
                .setRepos(this.publishRepos)
                .getMaven()
                    .getPomMetadata()
                        .setProjectUrl("https://jeka.dev")
                        .setScmUrl("https://github.com/jerkar/jeka.git")
                        .addApache2License();

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

}

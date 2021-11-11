import dev.jeka.core.CoreBuild;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.publication.JkNexusRepos;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.tooling.JkGitProcess;
import dev.jeka.core.tool.*;
import dev.jeka.core.tool.builtins.project.JkPluginProject;
import dev.jeka.core.tool.builtins.release.JkPluginVersionFromGit;
import dev.jeka.core.tool.builtins.crypto.JkPluginGpg;
import dev.jeka.core.tool.builtins.repos.JkPluginNexus;

class MasterBuild extends JkClass {

    @JkEnv("OSSRH_USER")
    public String ossrhUser;

    @JkEnv("OSSRH_PWD")
    public String ossrhPwd;

    @JkEnv("GH_TOKEN")
    public String githubToken;

    final JkPluginGpg gpg = getPlugin(JkPluginGpg.class);

    final JkPluginNexus nexus = getPlugin(JkPluginNexus.class);

    final JkPluginVersionFromGit versionFromGit = getPlugin(JkPluginVersionFromGit.class);

    // ------ Slave projects

    @JkDefImport("../dev.jeka.core")
    CoreBuild coreBuild;

    @JkDefImport("../plugins/dev.jeka.plugins.jacoco")
    JacocoPluginBuild jacocoBuild;

    @JkDefImport("../plugins/dev.jeka.plugins.sonarqube")
    JkClass sonarqubeBuild;

    @JkDefImport("../plugins/dev.jeka.plugins.springboot")
    JkClass springbootBuild;

    private JkRepoSet publishRepos;


    @Override
    protected void setup() throws Exception {
        versionFromGit.autoConfigureProject = false;
        coreBuild.runIT = true;
        publishRepos = JkRepoSet.ofOssrhSnapshotAndRelease(ossrhUser, ossrhPwd, gpg.get().getSigner(""));
        getImportedJkClasses().getDirect(JkPluginProject.class).forEach(this::configureSlave);
    }

    @JkDoc("Clean build of core and plugins + running all tests + publish if needed.")
    public void make() {
        JkLog.startTask("Building core and plugins");
        getImportedJkClasses().getDirect(JkPluginProject.class).forEach(projectPlugin -> {
            JkLog.startTask("Building " + projectPlugin.getJkClass());
            projectPlugin.getJkClass().clean();
            projectPlugin.pack();
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
            getImportedJkClasses().getDirect(JkPluginProject.class).forEach(plugin -> plugin.publish());
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
        getImportedJkClasses().getDirect(JkPluginProject.class).forEach(plugin -> {
            plugin.getProject().simpleFacade().setTestSkipped(true);
            plugin.getProject().getPublication().includeJavadocAndSources(false);
            plugin.getJkClass().clean();
            plugin.getProject().getPublication().pack();
        });
    }

    private void configureSlave(JkPluginProject projectPlugin) {
        versionFromGit.configure(projectPlugin.getProject(), false);
        if (!versionFromGit.version().isSnapshot()) {     // Produce javadoc only for releasej
            projectPlugin.pack.javadoc = true;
        }
        projectPlugin.getProject().getPublication().getMaven()
                .setVersion(versionFromGit.version())
                .setRepos(this.publishRepos)
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
        getImportedJkClasses().getDirect(JkPluginProject.class).forEach(pluginJava -> pluginJava.publishLocal());
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

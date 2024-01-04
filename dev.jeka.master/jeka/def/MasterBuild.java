import dev.jeka.core.CoreBuild;
import dev.jeka.core.api.crypto.gpg.JkGpg;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.depmanagement.publication.JkNexusRepos;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.tooling.JkGit;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.tool.*;
import dev.jeka.core.tool.builtins.git.GitKBean;
import dev.jeka.core.tool.builtins.git.JkVersionFromGit;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.repos.NexusKBean;
import dev.jeka.plugins.jacoco.JkJacoco;
import dev.jeka.plugins.sonarqube.SonarqubeKBean;
import github.Github;

import java.io.IOException;

@JkInjectClasspath("../plugins/dev.jeka.plugins.sonarqube/jeka/output/classes")
@JkInjectClasspath("../plugins/dev.jeka.plugins.jacoco/jeka/output/classes")
class MasterBuild extends KBean {

    @JkInjectProperty("OSSRH_USER")
    public String ossrhUser;

    @JkInjectProperty("OSSRH_PWD")
    public String ossrhPwd;

    @JkInjectProperty("GITHUB_TOKEN")
    public String githubToken;

    public boolean runSamples = true;

    private final JkVersionFromGit versionFromGit = JkVersionFromGit.of();


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

    private JkJacoco jacocoForCore;

    @Override
    protected void init()  {

        coreBuild.runIT = true;
        getImportedKBeans().get(ProjectKBean.class, false).forEach(this::applyToSlave);

        // For better self-testing, we instrument tests with Jacoco, even if sonarqube is not used.
        jacocoForCore = JkJacoco.ofVersion(getRuntime().getDependencyResolver(), JkJacoco.DEFAULT_VERSION);
        jacocoForCore.configureForAndApplyTo(coreBuild.load(ProjectKBean.class).project);

        load(NexusKBean.class).configureNexusRepo(this::configureNexus);
    }

    @JkDoc("Clean build of core and plugins + running all tests + publish if needed.")
    public void make() throws IOException {
        JkLog.startTask("Building core and plugins");
        getImportedKBeans().get(ProjectKBean.class, false).forEach(bean -> {
            JkLog.startTask("Running %s", bean);
            bean.clean();
            bean.pack();
            JkLog.endTask();
        });
        JkLog.endTask();
        if (runSamples) {
            JkLog.startTask("Running samples");
            SamplesTester samplesTester = new SamplesTester(this.getRuntime().getProperties());
            PluginScaffoldTester pluginScaffoldTester = new PluginScaffoldTester(this.getRuntime().getProperties());
            if (jacocoForCore != null) {
                samplesTester.setJacoco(jacocoForCore.getAgentJar(), jacocoForCore.getExecFile());
                pluginScaffoldTester.setJacoco(jacocoForCore.getAgentJar(), jacocoForCore.getExecFile());
            }
            samplesTester.run();
            pluginScaffoldTester.run();
            if (jacocoForCore != null) {
                jacocoForCore.generateExport();
            }
            JkLog.endTask();
        }
        getImportedKBeans().get(ProjectKBean.class, false).forEach(projectJkBean ->
                JkVersionFromGit.of().handleVersioning(projectJkBean.project));
        String branch = JkGit.of().getCurrentBranch();
        JkLog.trace("Current build branch: %s", branch);
        JkLog.trace("current ossrhUser:  %s", ossrhUser);

        // Publish artifacts only if we are on 'master' branch
        if (JkUtilsIterable.listOf("HEAD", "master").contains(branch) && ossrhUser != null) {
            JkLog.startTask("Publishing artifacts to Maven Central");
            getImportedKBeans().get(ProjectKBean.class, false).forEach(ProjectKBean::publish);
            closeAndReleaseRepo();
            JkLog.endTask();
            JkLog.startTask("Creating GitHub Release");
            Github github = new Github();
            github.ghToken = githubToken;
            github.publishGhRelease();
            JkLog.endTask();

            // If not on 'master' branch, publish only locally
        } else {
            JkLog.startTask("Publish locally");
            publishLocal();
            JkLog.endTask();
        }
        if (getRuntime().getProperties().get("sonar.host.url") != null) {
            coreBuild.load(SonarqubeKBean.class).run();
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
        JkNexusRepos.ofRepo(repo).closeAndRelease();
    }

    @JkDoc("Clean build of core + plugins bypassing tests.")
    public void buildFast() {
        getImportedKBeans().get(ProjectKBean.class, false).forEach(bean -> {
            bean.project.flatFacade().skipTests(true);
            bean.project.includeJavadocAndSources(false, false);
            bean.clean();
            bean.project.pack();
        });
    }

    private void configureNexus(JkNexusRepos nexusRepos) {
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

    private void applyToSlave(ProjectKBean projectKBean) {
        if (!JkVersion.of(versionFromGit.version()).isSnapshot()) {     // Produce javadoc only for release
            projectKBean.project.flatFacade().includeJavadocAndSources(true, true);
        }
        JkProject project = projectKBean.project;
        versionFromGit.handleVersioning(project);
        project.compilation
                        .addJavaCompilerOptions("-g");
        project.mavenPublication
                .setRepos(this.publishRepo())
                .pomMetadata
                    .setProjectUrl("https://jeka.dev")
                    .setScmUrl("https://github.com/jerkar/jeka.git")
                    .addApache2License();
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
        getImportedKBeans().get(ProjectKBean.class, false).forEach(ProjectKBean::publishLocal);
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
            System.out.println(JkInit.instanceOf(GitKBean.class, args).version());
        }
    }


}

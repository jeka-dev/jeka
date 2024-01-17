import dev.jeka.core.CoreBuild;
import dev.jeka.core.api.crypto.gpg.JkGpgSigner;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.publication.JkMavenPublication;
import dev.jeka.core.api.depmanagement.publication.JkNexusRepos;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.tooling.git.JkGit;
import dev.jeka.core.api.tooling.git.JkVersionFromGit;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.*;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.tooling.git.GitKBean;
import dev.jeka.core.tool.builtins.tooling.maven.MavenPublicationKBean;
import dev.jeka.core.tool.builtins.tooling.nexus.NexusKBean;
import dev.jeka.plugins.jacoco.JkJacoco;
import dev.jeka.plugins.sonarqube.SonarqubeKBean;
import github.Github;

import java.io.IOException;
import java.sql.SQLOutput;

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

    @JkInjectRunbase("../dev.jeka.core")
    CoreBuild coreBuild;

    @JkInjectRunbase("../plugins/dev.jeka.plugins.jacoco")
    JacocoBuild jacocoBuild;

    @JkInjectRunbase("../plugins/dev.jeka.plugins.sonarqube")
    SonarqubeBuild sonarqubeBuild;

    @JkInjectRunbase("../plugins/dev.jeka.plugins.springboot")
    SpringbootBuild springbootBuild;

    @JkInjectRunbase("../plugins/dev.jeka.plugins.nodejs")
    NodeJsBuild nodeJsBuild;

    @JkInjectRunbase("../plugins/dev.jeka.plugins.kotlin")
    KotlinBuild kotlinBuild;

    @JkInjectRunbase("../plugins/dev.jeka.plugins.protobuf")
    ProtobufBuild protobufBuild;

    private JkJacoco jacocoForCore;

    private final String effectiveVersion;

    MasterBuild() {
        // ON GitAction, when not on main branch, git version may return empty, so we rely on the branch name
        // injected in GitAction
        String gitVersion = versionFromGit.getVersion();
        effectiveVersion =  JkUtilsString.isBlank(gitVersion) ?
                System.getenv("CI_REF_NAME") + "-SNAPSHOT" :  gitVersion;
    }

    @Override
    protected void init()  {

        coreBuild.runIT = true;
        getImportedKBeans().get(ProjectKBean.class, false).forEach(this::applyToSlave);
        getImportedKBeans().get(MavenPublicationKBean.class, false).forEach(this::applyToSlave);

        // For better self-testing, we instrument tests with Jacoco, even if sonarqube is not used.
        jacocoForCore = JkJacoco.ofVersion(getRunbase().getDependencyResolver(), JkJacoco.DEFAULT_VERSION);
        jacocoForCore.configureForAndApplyTo(coreBuild.load(ProjectKBean.class).project);

        load(NexusKBean.class).configureNexusRepo(this::configureNexus);
    }

    @JkDoc("Clean build of core and plugins + running all tests + publish if needed.")
    public void make() throws IOException {

        System.out.println("==============================================");
        System.out.println("Version from Git  : " + JkVersionFromGit.of(getBaseDir(), "").getVersion());
        System.out.println("Branch from Git   : " + JkGit.of(getBaseDir()).getCurrentBranch());
        System.out.println("Tag rom Git       : " + JkGit.of(getBaseDir()).getTagsOfCurrentCommit());
        System.out.println("Effective version : " + effectiveVersion);
        System.out.println("==============================================");

        JkLog.startTask("Building core and plugins");
        getImportedKBeans().get(ProjectKBean.class, false).forEach(bean -> {
            JkLog.startTask("Packaging %s ...", bean);
            JkLog.info(bean.project.getInfo());
            bean.clean();
            bean.pack();
            JkLog.endTask();
        });
        JkLog.endTask();
        if (runSamples) {
            JkLog.startTask("Running samples");
            SamplesTester samplesTester = new SamplesTester(this.getRunbase().getProperties());
            PluginScaffoldTester pluginScaffoldTester = new PluginScaffoldTester(this.getRunbase().getProperties());
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
            getImportedKBeans().get(MavenPublicationKBean.class, false).forEach(MavenPublicationKBean::publish);
            bomPublication().publish();
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
        if (getRunbase().getProperties().get("sonar.host.url") != null) {
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
            bean.clean();
            bean.project.pack();
        });
    }

    @JkDoc("Publish all on local repo")
    public void publishLocal() {
        getImportedKBeans().get(MavenPublicationKBean.class, false).forEach(MavenPublicationKBean::publishLocal);
        bomPublication().publishLocal();
    }

    @JkDoc("Clean Pack jeka-core")
    public void buildCore() {
        coreBuild.cleanPack();
    }

    @JkDoc("Run samples")
    public void runSamples()  {
        new SamplesTester(this.getRunbase().getProperties()).run();
    }

    @JkDoc("Run scaffold test")
    public void runScaffoldsWithPlugins() {
        new PluginScaffoldTester(this.getRunbase().getProperties()).run();
    }

    private void configureNexus(JkNexusRepos nexusRepos) {
        nexusRepos.setReadTimeout(60*1000);
    }

    private JkRepoSet publishRepo() {
        JkRepo snapshotRepo = JkRepo.ofMavenOssrhDownloadAndDeploySnapshot(ossrhUser, ossrhPwd);
        JkGpgSigner gpg = JkGpgSigner.ofStandardProject(this.getBaseDir());
        JkRepo releaseRepo =  JkRepo.ofMavenOssrhDeployRelease(ossrhUser, ossrhPwd,  gpg);
        releaseRepo.publishConfig
                    .setVersionFilter(jkVersion -> !jkVersion.isSnapshot());
        JkRepo githubRepo = JkRepo.ofGitHub("jeka-dev", "jeka");
        githubRepo.publishConfig.setVersionFilter(jkVersion -> !jkVersion.isSnapshot());
        return  JkRepoSet.of(snapshotRepo, releaseRepo, githubRepo);
    }

    private void applyToSlave(ProjectKBean projectKBean) {
        JkProject project = projectKBean.project;
        project.setVersion(effectiveVersion);
        project.compilation.addJavaCompilerOptions("-g");
    }

    private void applyToSlave(MavenPublicationKBean mavenPublicationKBean) {
        adaptMavenConfig(mavenPublicationKBean.getMavenPublication());
    }

    private void adaptMavenConfig(JkMavenPublication mavenPublication) {
        mavenPublication
                .setRepos(this.publishRepo())
                .pomMetadata
            .setProjectUrl("https://jeka.dev")
            .setScmUrl("https://github.com/jerkar/jeka.git")
            .addApache2License();
    }

    private JkMavenPublication bomPublication() {
        JkMavenPublication result = JkMavenPublication.ofPomOnly();
        result.setModuleId("dev.jeka:bom")
                .setVersion(effectiveVersion)
                .pomMetadata
                    .setProjectName("Jeka BOM")
                    .setProjectDescription("Provides versions for all artifacts in 'dev.jeka' artifact group")
                    .addGithubDeveloper("djeang", "djeangdev@yahoo.fr");

        getImportedKBeans().get(ProjectKBean.class, false).forEach(projectKBean -> {
            JkProject project = projectKBean.project;
            result.addManagedDependenciesInPom(project.getModuleId().toColonNotation(), effectiveVersion);
        });
        adaptMavenConfig(result);
        return result;
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
            System.out.println(JkInit.instanceOf(GitKBean.class, args).gerVersionFromGit().getVersion());
        }
    }

}

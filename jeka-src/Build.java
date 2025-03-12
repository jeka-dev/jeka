import dev.jeka.core.CoreCustom;
import dev.jeka.core.api.crypto.gpg.JkGpgSigner;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.publication.JkMavenPublication;

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.tooling.git.JkGit;
import dev.jeka.core.api.tooling.git.JkVersionFromGit;
import dev.jeka.core.tool.*;
import dev.jeka.core.tool.builtins.base.BaseKBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.tooling.maven.MavenKBean;

import dev.jeka.plugins.nexus.JkNexusRepos;
import dev.jeka.plugins.nexus.NexusKBean;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@JkDep("org.junit.jupiter:junit-jupiter:5.12.0")
@JkDep("org.junit.platform:junit-platform-launcher:1.12.0")

@JkDep("dev.jeka:nexus-plugin:0.11.23")
class Build extends KBean {

    private static final String DOCKERHUB_TOKEN_ENV_NAME = "DOCKER_HUB_TOKEN";

    private static final String MKDOCS_OUTPUT_DIR= "jeka-output/mkdocs";

    @JkPropValue("OSSRH_USER")
    public String ossrhUser;

    @JkPropValue("OSSRH_PWD")
    public String ossrhPwd;

    @JkPropValue("GITHUB_TOKEN")
    public String githubToken;

    @JkPropValue("jeka.test.skip")
    public boolean skipTest = false;

    // ------ Child projects

    @JkInject("core")
    ProjectKBean coreProject;

    @JkInject("plugins/plugins.sonarqube")
    SonarqubeCustom sonarqubeBuild;

    @JkInject("plugins/plugins.jacoco")
    JacocoCustom jacocoBuild;

    @JkInject("plugins/plugins.springboot")
    SpringbootCustom springbootCustom;

    @JkInject("plugins/plugins.nodejs")
    NodeJsCustom nodeJsBuild;

    @JkInject("plugins/plugins.kotlin")
    KotlinCustom kotlinBuild;

    @JkInject("plugins/plugins.protobuf")
    ProtobufCustom protobufBuild;

    @JkInject("plugins/plugins.nexus")
    NexusCustom nexusBuild;

    private final String effectiveVersion;

    Build() {
        JkVersionFromGit versionFromGit = JkVersionFromGit.of();
        effectiveVersion = versionFromGit.getVersion();
    }

    @Override
    protected void init()  {
        getImportedKBeans().load(ProjectKBean.class, false)
                .forEach(this::configureCommonSettings);
        getImportedKBeans().load(MavenKBean.class, false)
                .forEach(this::configurePublication);
    }

    @JkDoc("Clean build of core and plugins + running all tests + publish if needed.")
    public void run() throws IOException {

        System.out.println("==============================================");
        System.out.println("Version from Git         : " + JkVersionFromGit.of(getBaseDir(), "").getVersion());
        System.out.println("Branch from Git          : " + computeBranchName());
        System.out.println("Tag from Git             : " + JkGit.of(getBaseDir()).getTagsOnCurrentCommit());
        System.out.println("Tag Count from Git       : " + JkGit.of(getBaseDir()).getTagsOnCurrentCommit().size());
        System.out.println("Effective version        : " + effectiveVersion);
        System.out.println("==============================================");

        // Build the core and plugin projects
        List<ProjectKBean> importedProjectKBeans = getImportedKBeans().get(ProjectKBean.class, false);
        importedProjectKBeans.forEach(projectKBean -> {
            JkLog.startTask("pack-and-test %s", projectKBean.project.getBaseDir().getFileName());
            projectKBean.clean();
            projectKBean.test();
            projectKBean.pack();
            projectKBean.e2eTest();
            JkLog.endTask();
        });

        // Run tests on sample projects if required
        if (!skipTest) {
            load(BaseKBean.class).test();
        }
        publish();
        enrichDoc();

    }

    @JkDoc("Convenient method to set Posix permission for all jeka shell files on git.")
    public void setPosixPermissions() {
        JkPathTree.of("../samples").andMatching("*/jeka", "**/jeka").getFiles().forEach(path -> {
            JkLog.info("Setting exec permission on git for file " + path);
            JkProcess.ofCmdLine("git update-index --chmod=+x " + path).run();
        });
    }

    @JkPostInit(required = true)
    private void postInit(NexusKBean nexusKBean) {
        nexusKBean.configureNexusRepo(nexusRepos -> nexusRepos.setReadTimeout(60 * 1000));
    }

    private void publish() throws IOException {
        if (shouldPublishOnMavenCentral()) {
            JkLog.info("current ossrhUser:  %s", ossrhUser);
            JkLog.startTask("publish-artifacts");
            getImportedKBeans().load(MavenKBean.class, false).forEach(MavenKBean::publish);
            bomPublication().publish();
            JkRepo repo = publishRepo().getRepoConfigHavingUrl(JkRepo.MAVEN_OSSRH_DEPLOY_RELEASE);
            JkNexusRepos.ofRepo(repo).closeAndRelease();
            JkLog.endTask();

            JkLog.startTask("create-github-release");
            Github github = new Github();
            github.ghToken = githubToken;
            github.publishGhRelease();
            JkLog.endTask();

            // Create a Docker Image of Jeka and publish it to docker hub
            if (System.getenv(DOCKERHUB_TOKEN_ENV_NAME) != null) {
                this.coreProject.load(CoreCustom.class).publishJekaDockerImage();
            }

        } else {
            JkLog.startTask("publish-locally");
            getImportedKBeans().load(MavenKBean.class, false).forEach(MavenKBean::publishLocal);
            bomPublication().publishLocal();
            JkLog.endTask();
        }
    }

    private boolean shouldPublishOnMavenCentral() {
        String branchOrTag = computeBranchName();
        if (branchOrTag != null &&
                (branchOrTag.startsWith("refs/tags/") || branchOrTag.equals("refs/heads/master"))
                && ossrhUser != null) {
            return true;
        }
        return false;
    }

    // For a few time ago, JkGit.of().getCurrentBranch() returns 'null' on githyb
    private static String computeBranchName() {
        String githubBranch = System.getenv("GITHUB_BRANCH");
        if (githubBranch != null) {
            return githubBranch;
        }
        return JkGit.of().getCurrentBranch();
    }

    private JkRepoSet publishRepo() {
        JkRepo snapshotRepo = JkRepo.ofMavenOssrhDownloadAndDeploySnapshot(ossrhUser, ossrhPwd);
        JkGpgSigner gpg = JkGpgSigner.ofStandardProperties();
        JkRepo releaseRepo =  JkRepo.ofMavenOssrhDeployRelease(ossrhUser, ossrhPwd,  gpg);
        releaseRepo.publishConfig
                    .setVersionFilter(jkVersion -> !jkVersion.isSnapshot());
        JkRepo githubRepo = JkRepo.ofGitHub("jeka-dev", "jeka");
        githubRepo.publishConfig.setVersionFilter(jkVersion -> !jkVersion.isSnapshot());
        return  JkRepoSet.of(snapshotRepo, releaseRepo, githubRepo);
    }

    private void configureCommonSettings(ProjectKBean projectKBean) {
        JkProject project = projectKBean.project;
        project.setVersion(effectiveVersion);
        project.compilation.addJavaCompilerOptions("-g");
        JkVersionFromGit.handleVersioning(project, "");
    }

    private void configurePublication(MavenKBean mavenKBean) {
        mavenKBean.customizePublication(this::configurePublication);
    }

    private void configurePublication(JkMavenPublication publication) {
        publication
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
        configurePublication(result);
        return result;
    }

    private void enrichDoc() {
        JkLog.startTask("enrich-mkdocs");
        String mkdocYmlFilename = "mkdocs.yml";
        Path baseDir = JkPathFile.of(mkdocYmlFilename).exists() ? Paths.get(".") : Paths.get("..");
        Path docBaseDir = baseDir.resolve("docs");
        Path generatedDocDir = baseDir.resolve(MKDOCS_OUTPUT_DIR).resolve("docs");

        JkPathTree.of(docBaseDir).copyTo(generatedDocDir, StandardCopyOption.REPLACE_EXISTING);
        JkPathFile.of(baseDir.resolve(mkdocYmlFilename)).copyToDir(generatedDocDir.getParent(),
                StandardCopyOption.REPLACE_EXISTING);
        new MkDocsEnricher(generatedDocDir).run();
        JkLog.endTask();
    }

    /**
     * Build + test + publish
     */
    public static void main(String[] args) throws Exception {
        JkInit.kbean(Build.class, args).run();
    }

}

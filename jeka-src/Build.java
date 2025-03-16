import dev.jeka.core.CoreCustom;
import dev.jeka.core.api.crypto.gpg.JkGpgSigner;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.publication.JkMavenPublication;

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.git.JkGit;
import dev.jeka.core.api.tooling.git.JkVersionFromGit;
import dev.jeka.core.tool.*;
import dev.jeka.core.tool.builtins.base.BaseKBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.tooling.maven.MavenKBean;

import dev.jeka.plugins.nexus.NexusKBean;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

@JkDep("org.junit.jupiter:junit-jupiter:5.12.0")
@JkDep("org.junit.platform:junit-platform-launcher:1.12.0")
@JkDep("dev.jeka:nexus-plugin:0.11.24")
@JkDep("core")

@JkChildBase("core")        // Forces core to be initialized prior plugin runbases
@JkChildBase("plugins/*")
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

    @JkInject("core")
    CoreCustom coreCustom;

    private final String effectiveVersion = JkVersionFromGit.of().getVersion();

    @Override
    protected void init()  {
        getRunbase().loadChildren(ProjectKBean.class).forEach(this::configCommon);
        getRunbase().loadChildren(MavenKBean.class).forEach(this::configCommon);
        getRunbase().loadChildren(NexusKBean.class).forEach(this::postInit);
    }

    @JkDoc("Clean build of core and plugins + running all tests + publish if needed.")
    public void run() throws IOException {

        getRunbase().loadChildren(ProjectKBean.class).forEach(projectKBean -> {
            JkLog.startTask("pack-and-test %s", projectKBean.project.getBaseDir().getFileName());
            JkProject project = projectKBean.project;
            project.clean();
            project.test.run();
            project.pack.run();
            project.e2eTest.run();
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
            JkGit.exec("update-index", "--chmod=+x", path);
        });
    }

    @JkPostInit(required = true)
    private void postInit(NexusKBean nexusKBean) {
        nexusKBean.setRepoReadTimeout(60 * 1000);
    }

    @JkPostInit(required = true)
    private void postInit(MavenKBean mavenKBean) {
        configCommon(mavenKBean);
        mavenKBean.customizePublication(this::configBomPublication);
    }

    private void publish() {
        if (shouldPublishOnOSSRH()) {

            JkLog.startTask("publish-ossrh");
            JkLog.info("current OSSRH user:  %s", ossrhUser);
            getRunbase().loadChildren(MavenKBean.class).forEach(MavenKBean::publish);
            load(MavenKBean.class).publish();
            JkLog.endTask();

            /*
            JkLog.startTask("create-github-release");
            Github github = new Github();
            github.ghToken = githubToken;
            github.publishGhRelease();
            JkLog.endTask();
             */

            // Create a Docker Image of Jeka and publish it to docker hub
            if (System.getenv(DOCKERHUB_TOKEN_ENV_NAME) != null) {
                coreCustom.publishJekaDockerImage();
            }

        } else {
            JkLog.startTask("publish-local");
            getRunbase().loadChildren(MavenKBean.class).forEach(MavenKBean::publishLocal);
            load(MavenKBean.class).publishLocal();
            JkLog.endTask();
        }
    }

    private boolean shouldPublishOnOSSRH() {
        String branchOrTag = computeBranchName();
        return branchOrTag != null
                && (branchOrTag.startsWith("refs/tags/") || branchOrTag.equals("refs/heads/master"))
                && ossrhUser != null;
    }

    // `JkGit.of().getCurrentBranch()` may return 'null' on GitHub Actions
    private static String computeBranchName() {
        return Optional.ofNullable(System.getenv("GITHUB_BRANCH"))
                .orElseGet(JkGit.of()::getCurrentBranch);
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

    private void configCommon(ProjectKBean projectKBean) {
        JkProject project = projectKBean.project;
        project.setVersion(effectiveVersion);
        project.compilation.addJavaCompilerOptions("-g");
        JkVersionFromGit.handleVersioning(project, "");
    }

    private void configCommon(MavenKBean mavenKBean) {
        mavenKBean.customizePublication(publication -> {
            publication
                    .setRepos(this.publishRepo())
                    .pomMetadata
                        .setProjectUrl("https://jeka.dev")
                        .setScmUrl("https://github.com/jerkar/jeka.git")
                        .addGithubDeveloper("djeang", "djeangdev@yahoo.fr")
                        .addApache2License();
        });
    }

    private void configBomPublication(JkMavenPublication bomPublication) {

        // Populate dependencyManagement section
        getRunbase().loadChildren(ProjectKBean.class).forEach(projectKBean -> {
            JkProject project = projectKBean.project;
            bomPublication.addManagedDependenciesInPom(project.getModuleId().toColonNotation(), effectiveVersion);
        });

        bomPublication
                .setModuleId("dev.jeka:bom")
                .setVersion(effectiveVersion)
                .pomMetadata
                    .setProjectName("Jeka BOM")
                    .setProjectDescription("Provides versions for all artifacts in 'dev.jeka' artifact group");

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

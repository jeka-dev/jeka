/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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
import dev.jeka.core.tool.builtins.tooling.git.GitKBean;
import dev.jeka.core.tool.builtins.tooling.maven.MavenKBean;

import dev.jeka.plugins.nexus.JkNexusRepos;
import dev.jeka.plugins.nexus.NexusKBean;
import test.SamplesTest;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@JkDep("org.junit.jupiter:junit-jupiter:5.12.0")
@JkDep("org.junit.platform:junit-platform-launcher:1.12.0")

@JkDep("plugins/plugins.nexus/jeka-output/classes")
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

    // ------ Slave projects

    @JkInject("core")
    ProjectKBean coreProject;

    @JkInject("plugins/plugins.sonarqube")
    ProjectKBean sonarqubeBuild;

    @JkInject("plugins/plugins.jacoco")
    JacocoBuild jacocoBuild;

    @JkInject("plugins/plugins.springboot")
    SpringbootCustom springbootCustom;

    @JkInject("plugins/plugins.nodejs")
    NodeJsBuild nodeJsBuild;

    @JkInject("plugins/plugins.kotlin")
    KotlinBuild kotlinBuild;

    @JkInject("plugins/plugins.protobuf")
    ProtobufBuild protobufBuild;

    @JkInject("plugins/plugins.nexus")
    NexusBuild nexusBuild;

    private final String effectiveVersion;

    Build() {
        JkVersionFromGit versionFromGit = JkVersionFromGit.of();
        effectiveVersion = versionFromGit.getVersion();
    }

    @Override
    protected void init()  {

        System.out.println("==============================================");
        System.out.println("Version from Git         : " + JkVersionFromGit.of(getBaseDir(), "").getVersion());
        System.out.println("Branch from Git          : " + computeBranchName());
        System.out.println("Tag from Git             : " + JkGit.of(getBaseDir()).getTagsOnCurrentCommit());
        System.out.println("Tag Count from Git       : " + JkGit.of(getBaseDir()).getTagsOnCurrentCommit().size());
        System.out.println("Effective version        : " + effectiveVersion);
        System.out.println("==============================================");

        getImportedKBeans().load(ProjectKBean.class, false)
                .forEach(this::configureCommonSettings);
        getImportedKBeans().load(MavenKBean.class, false)
                .forEach(this::configurePublication);
    }

    @JkDoc("Clean build of core and plugins + running all tests + publish if needed.")
    public void run() throws IOException {

        // Build core project then plugins
        JkLog.startTask("build-core-and-plugins");
        List<ProjectKBean> importedProjectKBeans = getImportedKBeans().get(ProjectKBean.class, false);
        importedProjectKBeans.forEach(projectKBean -> {
            JkLog.startTask("package %s", projectKBean);
            projectKBean.clean();
            projectKBean.test();
            projectKBean.pack();
            projectKBean.e2eTest();
            JkLog.endTask();
        });
        JkLog.endTask();

        // Run tests on sample projects if required
        if (!skipTest) {
            testSamples();
        }



        // Publish artifacts on maven central only if we are on 'master' branch

        publish();

        // Copy dir to and augment documentation
        JkLog.startTask("augment-mkdocs");
        this.copyDocsToWorkDir();
        JkLog.endTask();
    }

    private void publish() throws IOException {
        JkLog.info("Current build branch: %s", computeBranchName());
        if (shouldPublishOnMavenCentral()) {
            JkLog.info("current ossrhUser:  %s", ossrhUser);
            JkLog.startTask("publish-artifacts");
            getImportedKBeans().load(MavenKBean.class, false).forEach(MavenKBean::publish);
            bomPublication().publish();
            closeAndReleaseRepo();
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

            // If not on 'master' branch, publish only locally
        } else {
            JkLog.startTask("publish-locally");
            publishLocal();
            JkLog.endTask();
        }
    }

    @JkDoc("Convenient method to set Posix permission for all jeka shell files on git.")
    public void setPosixPermissions() {
        JkPathTree.of("../samples").andMatching("*/jeka", "**/jeka").getFiles().forEach(path -> {
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
            bean.project.flatFacade.setTestsSkipped(true);
            bean.clean();
            bean.project.pack();
        });
    }

    @JkDoc("publishes on local repo")
    public void publishLocal() {
        getImportedKBeans().load(MavenKBean.class, false).forEach(MavenKBean::publishLocal);
        bomPublication().publishLocal();
    }

    @JkDoc("Cleans Pack jeka-core")
    public void buildCore() {
        coreProject.clean();
        coreProject.pack();
    }

    @JkDoc("Tests samples")
    public void testSamples()  {
        load(BaseKBean.class).test();
    }

    @JkPostInit(required = true)
    private void postInit(NexusKBean nexusKBean) {
        nexusKBean.configureNexusRepo(nexusRepo ->
                nexusRepo.setReadTimeout(60*1000));
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

    private void copyDocsToWorkDir() {
        String mkdocYmlFilename = "mkdocs.yml";
        Path baseDir = JkPathFile.of(mkdocYmlFilename).exists() ? Paths.get(".") : Paths.get("..");
        Path docBaseDir = baseDir.resolve("docs");
        Path generatedDocDir = baseDir.resolve(MKDOCS_OUTPUT_DIR).resolve("docs");

        JkPathTree.of(docBaseDir).copyTo(generatedDocDir, StandardCopyOption.REPLACE_EXISTING);
        JkPathFile.of(baseDir.resolve(mkdocYmlFilename)).copyToDir(generatedDocDir.getParent(),
                StandardCopyOption.REPLACE_EXISTING);
        new MkDocsAugmenter(generatedDocDir).run();

    }

    /**
     * Build + test + publish
     */
    public static void main(String[] args) throws Exception {
        JkInit.kbean(Build.class, args).run();
    }

    static class BuildFast {
        public static void main(String[] args) {
            JkInit.kbean(Build.class, args).buildFast();
        }
    }

    static class ShowVersion {
        public static void main(String[] args) {
            System.out.println(JkInit.kbean(GitKBean.class, args).gerVersionFromGit().getVersion());
        }
    }

}

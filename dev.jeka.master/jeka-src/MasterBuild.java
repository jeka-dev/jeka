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

import dev.jeka.core.CoreBuild;
import dev.jeka.core.DockerImageMaker;
import dev.jeka.core.api.crypto.gpg.JkGpgSigner;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.publication.JkMavenPublication;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.tooling.git.JkGit;
import dev.jeka.core.api.tooling.git.JkVersionFromGit;
import dev.jeka.core.tool.*;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.tooling.git.GitKBean;
import dev.jeka.core.tool.builtins.tooling.maven.MavenKBean;

import dev.jeka.plugins.jacoco.JkJacoco;
import dev.jeka.plugins.nexus.JkNexusRepos;
import dev.jeka.plugins.nexus.NexusKBean;
import dev.jeka.plugins.sonarqube.SonarqubeKBean;
import github.Github;

import java.io.IOException;

@JkDep("../plugins/dev.jeka.plugins.sonarqube/jeka-output/classes")
@JkDep("../plugins/dev.jeka.plugins.jacoco/jeka-output/classes")
@JkDep("../plugins/dev.jeka.plugins.nexus/jeka-output/classes")
class MasterBuild extends KBean {

    private static final String DOCKERHUB_TOKEN_ENV_NAME = "DOCKER_HUB_TOKEN";

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

    @JkInjectRunbase("../plugins/dev.jeka.plugins.nexus")
    NexusBuild nexusBuild;

    private JkJacoco jacocoForCore;

    private final String effectiveVersion;

    MasterBuild() {
        effectiveVersion = versionFromGit.getVersion();
    }

    @Override
    protected void init()  {

        System.out.println("==============================================");
        System.out.println("Version from Git         : " + JkVersionFromGit.of(getBaseDir(), "").getVersion());
        System.out.println("Branch from Git          : " + computeBranchName());
        System.out.println("Tag from Git             : " + JkGit.of(getBaseDir()).getTagsOfCurrentCommit());
        System.out.println("Tag Count from Git       : " + JkGit.of(getBaseDir()).getTagsOfCurrentCommit().size());
        System.out.println("Effective version        : " + effectiveVersion);
        System.out.println("==============================================");

        coreBuild.runIT = true;
        getImportedKBeans().get(ProjectKBean.class, false).forEach(this::applyToSlave);
        getImportedKBeans().get(MavenKBean.class, false).forEach(this::applyToSlave);

        // For better self-testing, we instrument tests with Jacoco, even if sonarqube is not used.
        jacocoForCore = JkJacoco.ofVersion(getRunbase().getDependencyResolver(), JkJacoco.DEFAULT_VERSION);
        jacocoForCore.configureAndApplyTo(coreBuild.load(ProjectKBean.class).project);

        load(NexusKBean.class).configureNexusRepo(this::configureNexus);
    }

    @JkDoc("Clean build of core and plugins + running all tests + publish if needed.")
    public void make() throws IOException {

        // Build core project then plugins
        JkLog.startTask("build-core-and-plugins");
        getImportedKBeans().get(ProjectKBean.class, false).forEach(projectKBean -> {
            JkLog.startTask("package %s", projectKBean);
            JkLog.info(projectKBean.project.getInfo());
            projectKBean.clean();
            projectKBean.pack();
            JkLog.endTask();
        });
        JkLog.endTask();

        // Run tests on sample projects if required
        if (runSamples) {
            JkLog.startTask("run-samples");
            SamplesTester samplesTester = new SamplesTester();
            PluginScaffoldTester pluginScaffoldTester = new PluginScaffoldTester();

            // Instrument core with Jacoco when running sample tests
            if (jacocoForCore != null) {
                samplesTester.setJacoco(jacocoForCore.getAgentJar(), jacocoForCore.getExecFile());
                pluginScaffoldTester.setJacoco(jacocoForCore.getAgentJar(), jacocoForCore.getExecFile());
            }

            // run sample including springboot scaffolding project
            pluginScaffoldTester.run();

            // run samples
            samplesTester.run();

            if (jacocoForCore != null) {
                jacocoForCore.generateExport();
            }
            JkLog.endTask();
        }

        // Share same versioning for all sub-projects
        getImportedKBeans().get(ProjectKBean.class, false).forEach(
                projectJkBean -> JkVersionFromGit.of().handleVersioning(projectJkBean.project)
        );
        String branch = computeBranchName();
        JkLog.info("Current build branch: %s", branch);
        JkLog.info("current ossrhUser:  %s", ossrhUser);

        // Publish artifacts on maven central only if we are on 'master' branch
        if (shouldPublishOnMavenCentral()) {
            JkLog.startTask("Publishing artifacts to Maven Central");
            getImportedKBeans().get(MavenKBean.class, false).forEach(MavenKBean::publish);
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
                publishJekaDockerImage(effectiveVersion);
            }

            // If not on 'master' branch, publish only locally
        } else {
            JkLog.startTask("publish-locally");
            publishLocal();
            JkLog.endTask();
        }
        if (getRunbase().getProperties().get("sonar.host.url") != null) {
            coreBuild.load(SonarqubeKBean.class).run();
        }


        JkLog.info("Deploy Javadoc on gh-pages");
        coreBuild.pushJavadoc();;

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

    @JkDoc("publish-on-local-repo")
    public void publishLocal() {
        getImportedKBeans().get(MavenKBean.class, false).forEach(MavenKBean::publishLocal);
        bomPublication().publishLocal();
    }

    @JkDoc("Clean Pack jeka-core")
    public void buildCore() {
        coreBuild.cleanPack();
    }

    @JkDoc("Run samples")
    public void runSamples()  {
        new SamplesTester().run();
    }

    @JkDoc("Run scaffold test")
    public void runScaffoldsWithPlugins() {
        new PluginScaffoldTester().run();
    }

    private void configureNexus(JkNexusRepos nexusRepos) {
        nexusRepos.setReadTimeout(60*1000);
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

    private void applyToSlave(ProjectKBean projectKBean) {
        JkProject project = projectKBean.project;
        project.setVersion(effectiveVersion);
        project.compilation.addJavaCompilerOptions("-g");
    }

    private void applyToSlave(MavenKBean mavenKBean) {
        adaptMavenConfig(mavenKBean.getMavenPublication());
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

    private void publishJekaDockerImage(String version) {
        DockerImageMaker.createImage();
        DockerImageMaker.pushImage(version, System.getenv("DOCKER_HUB_TOKEN"));
    }

    /**
     * Build + test + publish
     */
    public static void main(String[] args) throws Exception {
        JkInit.kbean(MasterBuild.class, args).make();
    }

    static class BuildFast {
        public static void main(String[] args) {
            JkInit.kbean(MasterBuild.class, args).buildFast();
        }
    }

    static class ShowVersion {
        public static void main(String[] args) {
            System.out.println(JkInit.kbean(GitKBean.class, args).gerVersionFromGit().getVersion());
        }
    }

}

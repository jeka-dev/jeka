package dev.jeka.core.samples;

import dev.jeka.core.api.crypto.JkFileSigner;
import dev.jeka.core.api.crypto.gpg.JkGpgSigner;
import dev.jeka.core.api.depmanagement.JkPopularLibs;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.publication.JkMavenPublication;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.JkInjectProperty;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.tooling.maven.MavenPublicationKBean;

import java.nio.file.Path;

/**
 * When publishing on a public repository as Maven central, you need to provide extra metadata information, checksum
 * and signature files to the hosting repository. <p>
 *
 * This build demonstrates how to specify project metadata required to publish on
 * <ul>
 *     <li>Maven central ( see https://maven.apache.org/guides/mini/guide-central-repository-upload.html )</li>
 *     <li>On a local repo requiring same constraints (for repeatable run purpose) </li>
 * </ul>
 *
 * key name : jeka-dummy
 * key email : djeangdev@yahoo.fr
 * key passphrase : jeka-pwd
 *
 * @author Jerome Angibaud
 */
public class SignedArtifactsKBean extends KBean {

    ProjectKBean projectKBean = load(ProjectKBean.class);

    JkMavenPublication mavenPublication;

    @JkInjectProperty("OSSRH_USER")
    public String ossrhUser;  // OSSRH user and password will be injected from environment variables

    @JkInjectProperty("OSSRH_PWD")
    public String ossrhPwd;

    // A dummy local repository for repeatable run purpose
    private final Path dummyRepoPath = getOutputDir().resolve("test-output/maven-repo");

    // The key name to use from the keyStore bellow
    private final String keyName = "jeka-dummy";

    // The secret key file is stored in the project but protected with the password below.
    private final Path secringPath = getBaseDir().resolve("jekadummy-secring.gpg");

    public String secringPassword = "jeka-pwd";  // Normally injected from command line

    protected void init() {
        JkProject project = projectKBean.project;
        project.flatFacade()
            .customizeCompileDeps(deps -> deps
                .and(JkPopularLibs.GUAVA.toCoordinate("30.0-jre"))
            )
            .customizeTestDeps(deps -> deps
                .and(SimpleProjectKBean.JUNIT5)
            );

        mavenPublication = load(MavenPublicationKBean.class).getMavenPublication();
        mavenPublication
                .setModuleId("dev.jeka.core:samples-signedArtifacts")
                .setVersion("1.3.1")
                .setDefaultSigner(JkGpgSigner.of(secringPath, secringPassword, ""))
                .pomMetadata
                        .setProjectName("my project")
                        .setProjectDescription("My description")
                        .setProjectUrl("https://github.com/jerkar/jeka/samples")
                        .setScmConnection("https://github.com/jerkar/sample.git")
                        .addApache2License()
                        .addGithubDeveloper("John Doe", "johndoe6591@gmail.com");
        configForLocalRepo(mavenPublication);
    }

    private void configForOssrh(JkMavenPublication publication) {
        JkFileSigner signer = JkGpgSigner.of(secringPath, secringPassword, "");
        publication.setRepos(JkRepoSet.ofOssrhSnapshotAndRelease(ossrhUser, ossrhPwd, signer));
    }

    private void configForLocalRepo(JkMavenPublication publication) {
        JkRepo repo = JkRepo.of(dummyRepoPath);
        repo.publishConfig
                .setChecksumAlgos("sha1", "md5")
                .setSignatureRequired(true);
        publication.setRepos(repo.toSet());
    }

    public void cleanPackPublish() {
        JkPathTree.of(dummyRepoPath).createIfNotExist().deleteRoot();  // start from an empty repo
        projectKBean.cleanPack();
        mavenPublication.publish();
    }

    public static void main(String[] args) {
        JkInit.instanceOf(SignedArtifactsKBean.class, args).cleanPackPublish();
    }
   
}

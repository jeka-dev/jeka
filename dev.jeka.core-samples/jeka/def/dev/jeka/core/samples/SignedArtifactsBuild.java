package dev.jeka.core.samples;

import dev.jeka.core.api.crypto.gpg.JkGpg;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.project.JkJavaProjectPublication;
import dev.jeka.core.tool.JkClass;
import dev.jeka.core.tool.JkEnv;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.builtins.java.JkPluginJava;

import java.nio.file.Path;

import static dev.jeka.core.api.depmanagement.JkPopularModules.GUAVA;
import static dev.jeka.core.api.depmanagement.JkPopularModules.JUNIT;

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
public class SignedArtifactsBuild extends JkClass {

    JkPluginJava javaPlugin = getPlugin(JkPluginJava.class);

    @JkEnv("OSSRH_USER")
    public String ossrhUser;  // OSSRH user and password will be injected from environment variables

    @JkEnv("OSSRH_PWD")
    public String ossrhPwd;

    // A dummy local repository for repeatable run purpose
    private Path dummyRepoPath = getOutputDir().resolve("test-output/maven-repo");

    // The key name to use from the keyStore bellow
    private final String keyName = "jeka-dummy";

    // The secret key file is stored in the project but protected with the password below.
    private Path secringPath = getBaseDir().resolve("jeka/jekadummy-secring.gpg");

    public String secringPassword = "jeka-pwd";  // Normally injected from command line

    @Override
    protected void setup() {
        javaPlugin.getProject().simpleFacade()
            .setCompileDependencies(deps -> deps
                .and(GUAVA.version("18.0"))
            )
            .setTestDependencies(deps -> deps
                .and(JUNIT.version("4.13"))
            )
            .getProject()
            .getPublication()
                .setModuleId("dev.jeka.core:samples-signedArtifacts")
                .setVersion(JkVersion.of("1.3.1"))
                .setSigner(JkGpg.ofSecretRing(secringPath, secringPassword).getSigner(""))
                .apply(this::configForLocalRepo)
                .getMavenPublication()
                .getPomMetadata()
                    .getProjectInfo()
                        .setName("my project")
                        .setDescription("My description")
                        .setUrl("https://github.com/jerkar/jeka/samples").__
                    .getScm()
                        .setConnection("https://github.com/jerkar/sample.git").__
                    .addApache2License()
                    .addGithubDeveloper("John Doe", "johndoe6591@gmail.com");
    }

    private void configForOssrh(JkJavaProjectPublication publication) {
        publication
                .setRepos(JkRepoSet.ofOssrhSnapshotAndRelease(ossrhUser, ossrhPwd));
    }

    private void configForLocalRepo(JkJavaProjectPublication publication) {
        JkRepo repo = JkRepo.ofMaven(dummyRepoPath)
            .getPublishConfig()
                .setChecksumAlgos("sha1", "md5")
                .setSignatureRequired(true).__;
        publication.setRepos(repo.toSet());
    }

    public void cleanPackPublish() {
        JkPathTree.of(dummyRepoPath).createIfNotExist().deleteRoot();  // start from an empty repo
        clean(); javaPlugin.pack(); javaPlugin.getProject().getPublication().publish();
    }

    public static void main(String[] args) {
        JkInit.instanceOf(SignedArtifactsBuild.class, args).cleanPackPublish();
    }
   
}

package dev.jeka.core.samples;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.java.project.JkJavaProjectPublication;
import dev.jeka.core.tool.JkCommandSet;
import dev.jeka.core.tool.JkEnv;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.builtins.java.JkPluginJava;

import java.net.MalformedURLException;

import static dev.jeka.core.api.depmanagement.JkJavaDepScopes.TEST;
import static dev.jeka.core.api.depmanagement.JkPopularModules.GUAVA;
import static dev.jeka.core.api.depmanagement.JkPopularModules.JUNIT;

/**
 * When publishing on a public repository as Maven central, you need to provide extra metadata information, checksum
 * and signature files to hosting repository. <p>
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
public class SignedArtifactsBuild extends JkCommandSet {

    JkPluginJava javaPlugin = getPlugin(JkPluginJava.class);

    @JkEnv("OSSRH_USER")
    public String ossrhUser;  // OSSRH user and password will be injected from environment variables

    @JkEnv("OSSRH_PWD")
    public String ossrhPwd;

    @Override
    protected void setup() {
        javaPlugin.getProject()
            .getDependencyManagement()
                .addDependencies(JkDependencySet.of()
                    .and(GUAVA, "18.0")
                    .and(JUNIT, "4.13", TEST)).__
            .getPublication()
                .setModuleId("org.jerkar:sample-open-source")
                .setVersion(JkVersion.of("1.3.1"))
                //.apply(this::configForLocalRepo)
                .getPublishedPomMetadata()
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

    private void configForLocalRepo(JkJavaProjectPublication publication) throws MalformedURLException {
        JkRepo repo = JkRepo.ofMaven(getOutputDir().resolve("repo"))
                .getPublishConfig()
                    .setChecksumAlgos("sha2", "md5")
                    .setSignatureRequired(true).__;
        publication.setRepos(repo.toSet());
    }

    public void cleanPack() {
        clean(); javaPlugin.pack();
    }

    public static void main(String[] args) {
        JkInit.instanceOf(SignedArtifactsBuild.class, args).cleanPack();
    }
   
}

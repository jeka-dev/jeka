package org.jerkar.samples;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.jerkar.api.crypto.pgp.JkPgp;
import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkMavenPublication;
import org.jerkar.api.depmanagement.JkMavenPublicationInfo;
import org.jerkar.api.depmanagement.JkPublishRepo;
import org.jerkar.api.depmanagement.JkPublisher;
import org.jerkar.api.depmanagement.JkRepo;
import org.jerkar.api.depmanagement.JkVersionedModule;
import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.java.*;
import org.jerkar.api.java.junit.JkJavaTestSpec;
import org.jerkar.api.java.junit.JkUnit;
import org.jerkar.api.java.junit.JkUnit.JunitReportDetail;
import org.jerkar.tool.JkBuild;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.JkInit;

/**
 * Equivalent to http://ant.apache.org/manual/tutorial-HelloWorldWithAnt.html
 * 
 * @author Jerome Angibaud
 */
public class AntStyleBuild extends JkBuild {

    Path src = baseDir().resolve("src/main/java");
    Path buildDir = baseDir().resolve("build/output");
    Path classDir = outputDir().resolve("classes");
    Path jarFile = outputDir().resolve("jar/" + baseTree().root().getFileName() + ".jar");
    JkClasspath classpath = JkClasspath.ofMany(baseTree().accept("libs/**/*.jar").files());
    Path reportDir =buildDir.resolve("junitRreport");

    @Override
    public void doDefault() {
        clean();
        run();
    }

    public void compile() {
        JkJavaCompiler.base().compile(new JkJavaCompileSpec()
                .setOutputDir(classDir)
                .setClasspath(classpath)
                .addSources(src));
        JkPathTree.of(src).refuse("**/*.java").copyTo(classDir);
    }

    public void jar() {
        compile();
        JkManifest.empty().addMainClass("org.jerkar.samples.RunClass").writeToStandardLocation(classDir);
        JkPathTree.of(classDir).zipTo(jarFile);
    }

    public void run() {
        jar();
        JkJavaProcess.of().withWorkingDir(jarFile.getParent())
        .andClasspath(classpath)
        .runJarSync(jarFile);
    }

    public void cleanBuild() {
        clean();
        jar();
    }

    public void junit() {
        jar();
        JkUnit.of().forked()
        .withReportDir(reportDir)
        .withReport(JunitReportDetail.FULL)
        .run(JkJavaTestSpec.of(
                classpath.andMany(jarFile),
                JkPathTree.of(classDir).accept("**/*Test.class", "*Test.class") ));
    }

    /*
     * This part is specific to Maven publishing and does not exist in the
     * original helloWorld ANT file
     */
    @JkDoc("Redefine this value to set your own publish repository.")
    protected String publishRepo = "http://my/publish/repo";

    protected String pgpPrivateRingFile = "/usr/myUser/pgp/pub";

    protected String pgpPassword = "mypPgpPassword";

    public void publish() {
        JkPgp pgp = JkPgp.ofSecretRing(Paths.get(pgpPrivateRingFile), pgpPassword);
        JkPublishRepo repo = JkRepo.maven(publishRepo)
                .withCredential("myRepoUserName", "myRepoPassword")
                .asPublishRepo().withUniqueSnapshot(false).withSigner(pgp)
                .andSha1Md5Checksums();

        JkVersionedModule versionedModule = JkVersionedModule.of(
                "myGroup:myName", "0.2.1");

        // Optinal : if you need to add metadata in the generated pom
        JkMavenPublicationInfo info = JkMavenPublicationInfo
                .of("my project", "my description", "http://myproject.github")
                .withScm("http://scm/url/connection")
                .andApache2License()
                .andGitHubDeveloper("myName", "myName@provider.com");

        // Optional : if you want publish sources
        Path srcZip = outputDir().resolve("src.zip");
        JkPathTree.of(srcZip).zipTo(srcZip);

        JkMavenPublication publication = JkMavenPublication.of(jarFile)
                .with(info).and(srcZip, "sources");
        JkPublisher.of(repo).publishMaven(versionedModule, publication,
                JkDependencies.of());
    }

    public static void main(String[] args) {
        JkInit.instanceOf(AntStyleBuild.class, args).doDefault();
    }

}

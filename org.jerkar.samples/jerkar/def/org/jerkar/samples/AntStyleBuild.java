package org.jerkar.samples;

import org.jerkar.api.crypto.pgp.JkPgp;
import org.jerkar.api.depmanagement.*;
import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.java.*;
import org.jerkar.api.java.junit.JkJavaTestClasses;
import org.jerkar.api.java.junit.JkUnit;
import org.jerkar.api.java.junit.JkUnit.JunitReportDetail;
import org.jerkar.tool.JkRun;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.JkInit;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Equivalent to http://ant.apache.org/manual/tutorial-HelloWorldWithAnt.html
 * 
 * @author Jerome Angibaud
 */
public class AntStyleBuild extends JkRun {

    Path src = getBaseDir().resolve("src/main/java");
    Path buildDir = getBaseDir().resolve("build/output");
    Path classDir = getOutputDir().resolve("classes");
    Path jarFile = getOutputDir().resolve("jar/" + getBaseTree().getRoot().getFileName() + ".jar");
    JkClasspath classpath = JkClasspath.ofMany(getBaseTree().andAccept("libs/**/*.jar").getFiles());
    Path reportDir =buildDir.resolve("junitRreport");

    public void doDefault() {
        clean();
        run();
    }

    public void compile() {
        JkJavaCompiler.of().compile(new JkJavaCompileSpec()
                .setOutputDir(classDir)
                .setClasspath(classpath)
                .addSources(src));
        JkPathTree.of(src).andReject("**/*.java").copyTo(classDir);
    }

    public void jar() {
        compile();
        JkManifest.ofEmpty().addMainClass("org.jerkar.samples.RunClass").writeToStandardLocation(classDir);
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
        JkUnit.of().withForking()
        .withReportDir(reportDir)
        .withReport(JunitReportDetail.FULL)
        .run(JkJavaTestClasses.of(
                classpath.andPrepending(jarFile),
                JkPathTree.of(classDir).andAccept("**/*Test.class", "*Test.class") ));
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
        JkRepo repo = JkRepo.of(publishRepo)
                .withOptionalCredentials("myRepoUserName", "myRepoPassword");

        JkVersionedModule versionedModule = JkVersionedModule.of(
                "myGroup:myName:0.2.1");

        // Optinal : if you need to add metadata in the generated pom
        JkMavenPublicationInfo info = JkMavenPublicationInfo
                .of("my project", "my description", "http://myproject.github")
                .withScm("http://scm/url/connection")
                .andApache2License()
                .andGitHubDeveloper("myName", "myName@provider.com");

        // Optional : if you want publish sources
        Path srcZip = getOutputDir().resolve("src.zip");
        JkPathTree.of(srcZip).zipTo(srcZip);

        JkMavenPublication publication = JkMavenPublication.of(jarFile)
                .with(info).and(srcZip, "sources");
        JkPublisher.of(repo).publishMaven(versionedModule, publication,
                JkDependencySet.of());
    }

    public static void main(String[] args) {
        JkInit.instanceOf(AntStyleBuild.class, args).doDefault();
    }

}

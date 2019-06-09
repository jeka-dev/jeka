package dev.jeka.core.samples;

import dev.jeka.core.api.crypto.pgp.JkPgp;
import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.file.JkResourceProcessor;
import dev.jeka.core.api.java.*;
import dev.jeka.core.api.java.junit.JkJavaTestClasses;
import dev.jeka.core.api.java.junit.JkUnit;
import dev.jeka.core.api.java.junit.JkUnit.JunitReportDetail;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkImport;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.JkRun;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Equivalent to http://ant.apache.org/manual/tutorial-HelloWorldWithAnt.html
 * 
 * @author Jerome Angibaud
 */
@JkImport("org.apache.httpcomponents:httpclient:jar:4.5.6")
public class AntStyleBuild extends JkRun {

    Path src = getBaseDir().resolve("src/main/javaPlugin");
    Path buildDir = getBaseDir().resolve("build/output");
    Path classDir = getOutputDir().resolve("classes");
    Path jarFile = getOutputDir().resolve("jar/" + getBaseTree().getRoot().getFileName() + ".jar");
    JkClasspath classpath;
    Path reportDir = buildDir.resolve("junitRreport");

    public void doDefault() {
        clean();
        run();
    }

    @Override
    protected void setup() {
       JkResolveResult depResolution = JkDependencyResolver.of(JkRepo.ofMavenCentral()).resolve(JkDependencySet.of()
                .and("org.hibernate:hibernate-entitymanager:jar:5.4.2.Final")
       );
       classpath = JkClasspath.of(getBaseTree().andMatching(true,"libs/**/*.jar").getFiles())
            .and(depResolution.getFiles());
    }

    public void compile() {
        JkJavaCompiler.ofJdk().compile(JkJavaCompileSpec.of()
                .setOutputDir(classDir)
                .setClasspath(classpath)
                .setSourceAndTargetVersion(JkJavaVersion.V8)
                .addSources(src));
        Map<String, String> varReplacement = new HashMap<>();
        varReplacement.put("${server.ip}", "123.211.11.0");
        JkResourceProcessor.of(JkPathTreeSet.of(src)).andInterpolate("**/*.properties", varReplacement)
                .generateTo(classDir, Charset.forName("UTF-8"));
        JkPathTree.of(src).andMatching(false, "**/*.javaPlugin").copyTo(classDir);
    }

    public void jar() {
        compile();
        JkManifest.ofEmpty().addMainClass("RunClass").writeToStandardLocation(classDir);
        JkPathTree.of(classDir).zipTo(jarFile);
    }

    public void javadoc() {
        JkJavadocMaker.of(JkPathTreeSet.of(src), buildDir.resolve("javadoc")).process();
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
                JkPathTree.of(classDir).andMatching(true, "**/*Test.class", "*Test.class") ));
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

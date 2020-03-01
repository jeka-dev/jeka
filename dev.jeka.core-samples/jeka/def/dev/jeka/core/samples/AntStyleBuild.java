package dev.jeka.core.samples;

import dev.jeka.core.api.crypto.gpg.JkGpg;
import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.java.*;
import dev.jeka.core.api.java.junit.JkJavaTestClasses;
import dev.jeka.core.api.java.junit.JkUnit;
import dev.jeka.core.api.java.project.JkJavaProjectIde;
import dev.jeka.core.api.java.project.JkJavaProjectIdeSupplier;
import dev.jeka.core.api.java.project.JkProjectSourceLayout;
import dev.jeka.core.tool.JkCommandSet;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkDefClasspath;
import dev.jeka.core.tool.JkInit;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Equivalent to http://ant.apache.org/manual/tutorial-HelloWorldWithAnt.html
 * 
 * @author Jerome Angibaud
 */
@JkDefClasspath("org.apache.httpcomponents:httpclient:jar:4.5.6")
public class AntStyleBuild extends JkCommandSet implements JkJavaProjectIdeSupplier {

    Path src = getBaseDir().resolve("src/main/java");
    Path classDir = getOutputDir().resolve("classes");
    Path jarFile = getOutputDir().resolve("jar/" + getBaseTree().getRoot().getFileName() + ".jar");
    JkClasspath classpath;
    Path reportDir = getOutputDir().resolve("junitRreport");
    JkDependencyResolver resolver = JkDependencyResolver.of(JkRepo.ofMavenCentral());

    public JkDependencySet dependencies() {
        return JkDependencySet.of()
                .and("org.hibernate:hibernate-entitymanager:jar:5.4.2.Final")
                .and("junit:junit:4.11", JkJavaDepScopes.TEST);
    }

    public void doDefault() {
        clean();
        run();
    }

    @Override
    protected void setup() {
       JkResolveResult depResolution = resolver.resolve(dependencies());
       classpath = JkClasspath.of(getBaseTree().andMatching(true,"libs/**/*.jar").getFiles())
            .and(depResolution.getFiles());
    }

    public void compile() {
        JkJavaCompiler.ofJdk().compile(JkJavaCompileSpec.of()
                .setOutputDir(classDir)
                .setClasspath(classpath)
                .setSourceAndTargetVersion(JkJavaVersion.V8)
                .addSources(src));
        JkPathTree.of(src).andMatching(false, "**/*.java")
                .copyTo(classDir);
    }

    public void jar() {
        compile();
        JkManifest.ofEmpty().addMainClass("RunClass").writeToStandardLocation(classDir);
        JkPathTree.of(classDir).zipTo(jarFile);
    }

    public void javadoc() {
        JkJavadocMaker.of(JkPathTreeSet.of(src), getOutputDir().resolve("javadoc")).process();
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
        .withReport(JkUnit.JunitReportDetail.FULL)
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
        JkGpg pgp = JkGpg.ofSecretRing(Paths.get(pgpPrivateRingFile), pgpPassword);
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

    @Override
    public JkJavaProjectIde getJavaProjectIde() {
        return JkJavaProjectIde.ofDefault()
                .withSourceLayout(JkProjectSourceLayout.ofSimpleStyle()
                        .withSources(JkPathTreeSet.of(src))
                        .withBaseDir(getBaseDir()))
                .withDependencyResolver(resolver)
                .withDependencies(dependencies());
    }


    public static void main(String[] args) {
        JkInit.instanceOf(AntStyleBuild.class, args).doDefault();
    }


}

package dev.jeka.core.samples;

import dev.jeka.core.api.crypto.gpg.JkGpg;
import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.java.*;
import dev.jeka.core.api.java.project.JkJavaIdeSupport;
import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.tool.JkCommandSet;
import dev.jeka.core.tool.JkDefClasspath;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkInit;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Equivalent to http://ant.apache.org/manual/tutorial-HelloWorldWithAnt.html
 * 
 * @author Jerome Angibaud
 */
@JkDefClasspath("org.apache.httpcomponents:httpclient:jar:4.5.6")
public class AntStyleBuild extends JkCommandSet implements JkJavaIdeSupport.JkSupplier {

    Path src = getBaseDir().resolve("src/main/java");
    Path classDir = getOutputDir().resolve("classes");
    Path jarFile = getOutputDir().resolve("jar/" + getBaseTree().getRoot().getFileName() + ".jar");
    JkClasspath classpath;
    Path reportDir = getOutputDir().resolve("junitRreport");
    JkDependencyResolver resolver = JkDependencyResolver.ofParent(JkRepo.ofMavenCentral());

    public JkDependencySet dependencies() {
        return JkDependencySet.of()
                .and("org.hibernate:hibernate-entitymanager:jar:5.4.2.Final")
                .and("junit:junit:4.13", JkJavaDepScopes.TEST);
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
        JkJavaCompiler.of().compile(JkJavaCompileSpec.of()
                .setOutputDir(classDir)
                .setClasspath(classpath)
                .setSourceAndTargetVersion(JkJavaVersion.V8)
                .addSources(src));
        JkPathTree.of(src).andMatching(false, "**/*.java")
                .copyTo(classDir);
    }

    public void jar() {
        compile();
        JkManifest.of().addMainClass("RunClass").writeToStandardLocation(classDir);
        JkPathTree.of(classDir).zipTo(jarFile);
    }

    public void javadoc() {
        JkJavadocProcessor.of()
                .make(JkClasspath.of(), JkPathTreeSet.of(src), getOutputDir().resolve("javadoc"));
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
        JkPublishedPomMetadata info = JkPublishedPomMetadata.of()
            .getProjectInfo()
                .setName("my project")
                .setDescription("My description")
                .setUrl("https://github.com/jerkar/jeka/samples").__
            .getScm()
                .setConnection("https://github.com/jerkar/sample.git").__
            .addApache2License()
            .addGithubDeveloper("John Doe", "johndoe6591@gmail.com");

        JkArtifactLocator artifactLocator = JkArtifactBasicProducer.of()
                .putMainArtifact(path -> JkPathFile.of(jarFile).move(path))
                .putArtifact(JkJavaProject.SOURCES_ARTIFACT_ID, path -> JkPathTree.of(this.src).zipTo(path));


        JkPublisher.of(repo).publishMaven(versionedModule, JkMavenPublication.of(artifactLocator, info),
                JkDependencySet.of());
    }

    @Override
    public JkJavaIdeSupport getJavaIdeSupport() {
        return JkJavaIdeSupport.of(getBaseDir())
            .getProdLayout()
                .emptySources()
                .addSource(src).__
            .setDependencies(dependencies())
            .setDependencyResolver(resolver);
    }


    public static void main(String[] args) {
        JkInit.instanceOf(AntStyleBuild.class, args).doDefault();
    }


}

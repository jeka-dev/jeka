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
import dev.jeka.core.tool.JkInit;

import java.nio.file.Path;
import java.util.List;

/**
 * Build class written in Ant style. Tasks are explicitly written in public methods using
 * low level API. This approach
 * 
 * @author Jerome Angibaud
 */
@JkDefClasspath("org.apache.httpcomponents:httpclient:jar:4.5.6")
public class AntStyleBuild extends JkCommandSet implements JkJavaIdeSupport.JkSupplier {

    Path src = getBaseDir().resolve("src/main/java");
    Path classDir = getOutputDir().resolve("classes");
    Path jarFile = getOutputDir().resolve("jar/" + getBaseTree().getRoot().getFileName() + ".jar");
    JkDependencyResolver resolver = JkDependencyResolver.of().addRepos(JkRepo.ofMavenCentral());
    JkDependencySet moduleDependencies = JkDependencySet.of()
            .and("org.hibernate:hibernate-entitymanager:jar:5.4.2.Final")
            .and("junit:junit:4.13", JkJavaDepScopes.TEST);
    List<Path> depFiles = getBaseTree().andMatching(true,"libs/**/*.jar").getFiles();
    JkResolveResult depResolution = resolver.resolve(moduleDependencies);
    JkClasspath classpath = JkClasspath
            .of(depFiles)
            .and(depResolution.getFiles());

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
        javadoc();
        publish();
    }

    public void publish() {
        JkGpg pgp = JkGpg.ofSecretRing(getBaseDir().resolve("jeka/jekadummy-secring.gpg"), "jeka-pwd");
        JkRepo repo = JkRepo.ofIvy(getOutputDir().resolve("ivy-repo"));
        JkVersionedModule versionedModule = JkVersionedModule.of("myGroup:myName:0.2.2_SNAPSHOT");
        JkArtifactLocator artifactLocator = JkArtifactBasicProducer.of(getOutputDir(), "mygroup.myname")
                .putMainArtifact(path -> JkPathFile.of(jarFile).move(path))
                .putArtifact(JkJavaProject.SOURCES_ARTIFACT_ID, path -> JkPathTree.of(this.src).zipTo(path));
        JkPublisher.of(repo)
                .withSigner(pgp.getSigner(""))
                .publishMaven(versionedModule, JkMavenPublication.of(artifactLocator, JkPublishedPomMetadata.of()),
                        JkDependencySet.of());
    }

    @Override
    public JkJavaIdeSupport getJavaIdeSupport() {
        return JkJavaIdeSupport.of(getBaseDir())
            .getProdLayout()
                .emptySources().addSource(src).__
            .setDependencies(moduleDependencies)
            .setDependencyResolver(resolver);
    }

    public static void main(String[] args) {
        JkInit.instanceOf(AntStyleBuild.class, args).cleanBuild();
    }

}

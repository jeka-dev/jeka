package dev.jeka.core.samples;

import dev.jeka.core.api.crypto.gpg.JkGpg;
import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.java.*;
import dev.jeka.core.api.java.project.JkJavaIdeSupport;
import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.tool.JkCommandSet;
import dev.jeka.core.tool.JkDefClasspath;
import dev.jeka.core.tool.JkInit;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/**
 * Build class written in Ant style. This is not expected to be the most common way of creating build class in Jeka.
 * <p>
 * Tasks are explicitly written in public methods using a low level API. This approach may be more flexible but requires
 * extra coding effort.
 * 
 * @author Jerome Angibaud
 */
@JkDefClasspath("org.apache.httpcomponents:httpclient:jar:4.5.6")
public class AntStyleBuild extends JkCommandSet implements JkJavaIdeSupport.JkSupplier {

    Path src = getBaseDir().resolve("src/main/java");
    Path srcJar = getOutputDir().resolve("jar/" + getBaseTree().getRoot().getFileName() + "-sources.jar");
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

    public void jarSources() {
        JkPathTree.of(src).zipTo(srcJar);
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

    public void cleanPackPublish() {
        clean();
        jar();
        javadoc();
        publish();
    }

    // publish poth on Maven and Ivy repo
    public void publish() {
        JkGpg pgp = JkGpg.ofSecretRing(getBaseDir().resolve("jeka/jekadummy-secring.gpg"), "jeka-pwd");
        JkRepo ivyRepo = JkRepo.ofIvy(getOutputDir().resolve("test-output/ivy-repo"));
        JkRepo mavenRepo = JkRepo.ofMaven(getOutputDir().resolve("test-output/maven-repo"));
        JkVersionedModule versionedModule = JkVersionedModule.of("myGroup:myName:0.2.2_SNAPSHOT");
        JkArtifactProducer artifactProducer = JkSuppliedFileArtifactProducer.of()
                .putMainArtifact(jarFile, this::jar)
                .putArtifact(JkJavaProject.SOURCES_ARTIFACT_ID, srcJar, this::jarSources);
        artifactProducer.makeAllMissingArtifacts();
        JkIvyPublication ivyPublication = JkIvyPublication.of(artifactProducer);
        JkPublisher.of(JkRepoSet.of(mavenRepo, ivyRepo))
                .withSigner(pgp.getSigner(""))
                .publishMaven(versionedModule, JkMavenPublication.of(artifactProducer, JkPublishedPomMetadata.of()),
                        moduleDependencies)
                .publishIvy(versionedModule, ivyPublication, moduleDependencies, JkJavaDepScopes.DEFAULT_SCOPE_MAPPING,
                        Instant.now(), JkVersionProvider.of());
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
        JkInit.instanceOf(AntStyleBuild.class, args).cleanPackPublish();
    }

}

package dev.jeka.core.samples;

import dev.jeka.core.api.crypto.gpg.JkGpg;
import dev.jeka.core.api.depmanagement.JkCoordinateDependency;
import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactProducer;
import dev.jeka.core.api.depmanagement.artifact.JkSuppliedFileArtifactProducer;
import dev.jeka.core.api.depmanagement.publication.JkIvyPublication;
import dev.jeka.core.api.depmanagement.publication.JkMavenPublication;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.java.*;
import dev.jeka.core.api.project.JkIdeSupport;
import dev.jeka.core.api.project.JkIdeSupportSupplier;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.JkInjectClasspath;
import dev.jeka.core.tool.builtins.ide.IntellijJkBean;

import java.nio.file.Path;
import java.util.List;

/**
 * Build class written in Ant style. This is not expected to be the most common way of creating build class in Jeka.
 * <p>
 * Tasks are explicitly written in public methods using a low level API. This approach may be more flexible but requires
 * extra coding effort.
 * 
 * @author Jerome Angibaud
 */
@JkInjectClasspath("org.apache.httpcomponents:httpclient:4.5.6")
public class AntStyleJkBean extends JkBean implements JkIdeSupportSupplier {

    final IntellijJkBean intellijJkBean = getBean(IntellijJkBean.class)
            .configureIml(jkIml -> {
                jkIml.component.replaceLibByModule("dev.jeka.jeka-core.jar", "dev.jeka.core");
            });

    Path src = getBaseDir().resolve("src/main/java");
    Path test = getBaseDir().resolve("src/test/java");
    JkPathTree baseTree = JkPathTree.of(getBaseDir());
    Path srcJar = getOutputDir().resolve("jar/" + baseTree.getRoot().getFileName() + "-sources.jar");
    Path classDir = getOutputDir().resolve("classes");
    Path jarFile = getOutputDir().resolve("jar/" + baseTree.getRoot().getFileName() + ".jar");
    JkDependencyResolver resolver = JkDependencyResolver.of().addRepos(JkRepo.ofMavenCentral());
    JkDependencySet prodDependencies = JkDependencySet.of()
            .and("com.google.guava:guava:30.0-jre")
            .and("org.hibernate:hibernate-entitymanager:5.6.15.Final");
    JkDependencySet testDependencies = JkDependencySet.of()
            .and(SimpleProjectJkBean.JUNIT5);
    List<Path> depFiles = baseTree.andMatching(true,"libs/**/*.jar").getFiles();
    JkPathSequence prodClasspath = resolver.resolve(prodDependencies).getFiles();
    JkPathSequence testClasspath = resolver.resolve(testDependencies.and(prodDependencies)).getFiles();

    public void compile() {
        JkPathTree javaSources = JkPathTree.of(src).andMatching(false, "**/*.java");
        JkJavaCompiler.of().compile(JkJavaCompileSpec.of()
                .setOutputDir(classDir)
                .setClasspath(prodClasspath)
                .setSourceVersion("8")
                .setTargetVersion("8")
                .setSources(javaSources.toSet()));
        JkPathTree resources =   JkPathTree.of(src).andMatching(false, "**/*.java");
        resources.copyTo(classDir);
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
            .make(JkClasspath.of(), JkPathTreeSet.ofRoots(src), getOutputDir().resolve("javadoc"));
    }

    public void run() {
        jar();
        JkJavaProcess.ofJavaJar(jarFile, null)
                .setWorkingDir(jarFile.getParent())
                .setClasspath(prodClasspath)
                .exec();
    }

    public void cleanPackPublish() {
        cleanOutput();
        jar();
        javadoc();
        publish();
    }

    // publish both on Maven and Ivy repo
    public void publish() {
        JkGpg pgp = JkGpg.ofSecretRing(getBaseDir().resolve("jeka/jekadummy-secring.gpg"), "jeka-pwd");
        JkRepo ivyRepo = JkRepo.of(getOutputDir().resolve("test-output/ivy-repo"));
        JkRepo mavenRepo = JkRepo.of(getOutputDir().resolve("test-output/maven-repo"));
        JkCoordinateDependency versionedModule = JkCoordinateDependency.of("myGroup:myName:0.2.2-SNAPSHOT");
        JkArtifactProducer artifactProducer = JkSuppliedFileArtifactProducer.of()
                .putMainArtifact(jarFile, this::jar)
                .putArtifact(JkProject.SOURCES_ARTIFACT_ID, srcJar, this::jarSources);
        artifactProducer.makeAllMissingArtifacts();
        mavenRepo.publishConfig.setSigner(pgp.getSigner(""));
        JkMavenPublication.of()
                .setArtifactLocator(artifactProducer)
                .configureDependencies(deps -> prodDependencies)
                .setModuleId(versionedModule.getCoordinate().getModuleId().toString())
                .setVersion(versionedModule.getCoordinate().getVersion().getValue())
                .addRepos(mavenRepo)
                .publish();
        JkIvyPublication.of()
                .setModuleId(versionedModule.getCoordinate().getModuleId().toString())
                .setVersion(versionedModule.getCoordinate().getVersion().getValue())
                .setDependencies(prodDependencies, prodDependencies, testDependencies)
                .addArtifacts(artifactProducer)
                .addRepos(ivyRepo)
                .publish();
    }

    @Override
    public JkIdeSupport getJavaIdeSupport() {
        JkIdeSupport result = JkIdeSupport.of(getBaseDir());
        result
            .getProdLayout()
                .emptySources()
                .addSource(src);
        result
            .getTestLayout()
                .emptySources()
                .addSource(test);
        result
            .setDependencies(prodDependencies, prodDependencies, testDependencies)
            .setDependencyResolver(resolver);
        return result;
    }

    public static void main(String[] args) {
        JkInit.instanceOf(AntStyleJkBean.class, args).cleanPackPublish();
    }

}

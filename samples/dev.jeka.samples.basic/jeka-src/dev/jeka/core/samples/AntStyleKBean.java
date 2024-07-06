package dev.jeka.core.samples;

import dev.jeka.core.api.crypto.gpg.JkGpgSigner;
import dev.jeka.core.api.depmanagement.JkCoordinateDependency;
import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactLocator;
import dev.jeka.core.api.depmanagement.publication.JkIvyPublication;
import dev.jeka.core.api.depmanagement.publication.JkMavenPublication;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.java.*;
import dev.jeka.core.api.project.JkIdeSupport;
import dev.jeka.core.api.project.JkIdeSupportSupplier;
import dev.jeka.core.api.tooling.intellij.JkIml;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.JkInjectClasspath;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.tooling.ide.IntellijKBean;

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
public class AntStyleKBean extends KBean implements JkIdeSupportSupplier {

    AntStyleKBean() {
        load(IntellijKBean.class)
                .replaceLibByModule("dev.jeka.jeka-core.jar", "dev.jeka.core")
                .setModuleAttributes("dev.jeka.core", JkIml.Scope.COMPILE, false )
        ;

    }

    Path src = getBaseDir().resolve("src/main/java");
    Path test = getBaseDir().resolve("src/test/java");
    JkPathTree baseTree = JkPathTree.of(getBaseDir());

    Path classDir = getOutputDir().resolve("classes");

    JkArtifactLocator artifactLocator= JkArtifactLocator.of(getOutputDir(), "sample-ant-style");

    Path jarFile = artifactLocator.getMainArtifactPath();

    Path srcJarFile = artifactLocator.getArtifactPath("source", "jar");

    JkDependencyResolver resolver = JkDependencyResolver.of().addRepos(JkRepo.ofMavenCentral());
    JkDependencySet prodDependencies = JkDependencySet.of()
            .and("com.google.guava:guava:30.0-jre")
            .and("org.hibernate:hibernate-entitymanager:5.6.15.Final");
    JkDependencySet testDependencies = JkDependencySet.of()
            .and(SimpleProjectKBean.JUNIT5);
    List<Path> depFiles = baseTree.andMatching(true,"libs/**/*.jar").getFiles();
    JkPathSequence prodClasspath = resolver.resolve(prodDependencies).getFiles();
    JkPathSequence testClasspath = resolver.resolve(testDependencies.and(prodDependencies)).getFiles();

    public void compile() {
        JkPathTree javaSources = JkPathTree.of(src).andMatching(false, "**/*.java");
        JkJavaCompilerToolChain.of().compile(JkJavaCompileSpec.of()
                .setOutputDir(classDir)
                .setClasspath(prodClasspath)
                .setSourceVersion("8")
                .setTargetVersion("8")
                .setSources(javaSources.toSet()));
        JkPathTree resources = JkPathTree.of(src).andMatching(false, "**/*.java");
        resources.copyTo(classDir);
    }

    public void jarSources(Path target) {
        JkPathTree.of(src).zipTo(target);
    }

    public void makeJar(Path target) {
        compile();
        JkManifest.of().addMainClass("RunClass").writeToStandardLocation(classDir);
        JkPathTree.of(classDir).zipTo(target);
    }

    public void javadoc() {
        JkJavadocProcessor.of()
            .make(JkPathSequence.of(), JkPathTreeSet.ofRoots(src), getOutputDir().resolve("javadoc"));
    }

    public void run() {
        makeJar(jarFile);
        JkJavaProcess.ofJavaJar(jarFile, null)
                .setWorkingDir(jarFile.getParent())
                .setClasspath(prodClasspath)
                .exec();
    }

    public void cleanPackPublish() {
        cleanOutput();
        makeJar(jarFile);
        javadoc();
        publish();
    }

    // publish both on Maven and Ivy repo
    public void publish() {

        JkGpgSigner gpg = JkGpgSigner.ofSecretRing(getBaseDir().resolve("jekadummy-secring.gpg"), "jeka-pwd", "");
        JkRepo ivyRepo = JkRepo.of(getOutputDir().resolve("test-output/ivy-repo"));
        JkRepo mavenRepo = JkRepo.of(getOutputDir().resolve("test-output/maven-repo"));
        JkCoordinateDependency versionedModule = JkCoordinateDependency.of("myGroup:myName:0.2.2-SNAPSHOT");

        mavenRepo.publishConfig.setSigner(gpg);

        JkMavenPublication.of(artifactLocator)
                .putArtifact(JkArtifactId.MAIN_JAR_ARTIFACT_ID, this::makeJar)
                .putArtifact(JkArtifactId.SOURCES_ARTIFACT_ID, this::jarSources)
                .customizeDependencies(deps -> prodDependencies)
                .setModuleId(versionedModule.getCoordinate().getModuleId().toString())
                .setVersion(versionedModule.getCoordinate().getVersion().getValue())
                .addRepos(mavenRepo)
                .publish();

        JkIvyPublication.of()
                .putMainArtifact(jarFile)
                .putArtifact("sources", srcJarFile)
                .setModuleIdSupplier(versionedModule.getCoordinate()::getModuleId)
                .setVersion(versionedModule.getCoordinate().getVersion().getValue())
                .setDependencies(prodDependencies, prodDependencies, testDependencies)
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
        JkInit.kbean(AntStyleKBean.class, args).cleanPackPublish();
    }

}

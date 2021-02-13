package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import dev.jeka.core.api.depmanagement.resolution.JkResolvedDependencyNode;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.api.system.JkLog;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JkDependencyResolverTest {

    @Test
    public void resolve_only1ModuleDependencies_ok() throws Exception {
        JkLog.Verbosity verbosity = JkLog.verbosity();
        JkDependencySet dependencies = JkDependencySet.of()
                .and(JkPopularModules.GUAVA + ":" + TestConstants.GUAVA_VERSION);
        JkDependencyResolver dependencyResolver = JkDependencyResolver.of().addRepos(JkRepo.ofMavenCentral());
        JkResolveResult resolveResult = dependencyResolver.resolve(dependencies);
        resolveResult.assertNoError();
        Assert.assertEquals(1, resolveResult.getDependencyTree().getChildren().size());
        JkLog.setVerbosity(verbosity);
    }

    @Test
    public void resolve_dependencyOfTypeProject_resultWithProjectIdeDir() throws Exception {
        Path root = unzipToDir("sample-multiproject.zip");

        JkJavaProject baseProject = JkJavaProject.of().simpleFacade()
            .setBaseDir(root.resolve("base"))
            .addCompileDependencies(JkDependencySet.of()
                .and("com.google.guava:guava:" + TestConstants.GUAVA_VERSION)).getProject();

        JkJavaProject coreProject = JkJavaProject.of().simpleFacade()
            .setBaseDir(root.resolve("core"))
            .addCompileDependencies(JkDependencySet.of(baseProject.toDependency())).getProject();

        JkResolveResult resolveResult = coreProject.getConstruction().getDependencyResolver().resolve(
                coreProject.getConstruction().getCompilation().getDependencies());

        Assert.assertEquals(2, resolveResult.getDependencyTree().getChildren().size()); // base dir and guava
        JkResolvedDependencyNode dependencyNode = resolveResult.getDependencyTree().getChildren().get(0);
        Assert.assertFalse(dependencyNode.isModuleNode());
        JkResolvedDependencyNode.JkFileNodeInfo nodeInfo = (JkResolvedDependencyNode.JkFileNodeInfo) dependencyNode.getNodeInfo();
        Assert.assertEquals(baseProject.getBaseDir(), nodeInfo.computationOrigin().getIdeProjectDir());
    }

    @Test
    public void resolve_fileAndModuleDependencies_ok() throws Exception {
        URL sampleJarUrl = JkDependencyResolverTest.class.getResource("myArtifactSample.jar");
        Path jarFile = Paths.get(sampleJarUrl.toURI());
        JkDependencySet dependencies = JkDependencySet.of()
                .and(JkPopularModules.GUAVA.version(TestConstants.GUAVA_VERSION))
                .andFiles(jarFile);
        JkDependencyResolver dependencyResolver = JkDependencyResolver.of().addRepos(JkRepo.ofMavenCentral());
        JkResolveResult resolveResult = dependencyResolver.resolve(dependencies);
        Assert.assertEquals(2, resolveResult.getDependencyTree().getChildren().size());
        resolveResult.assertNoError();
    }

    @Test
    public void resolve_onlyFilesDependencies_ok() throws Exception {
        URL sampleJarUrl = JkDependencyResolverTest.class.getResource("myArtifactSample.jar");
        Path jarFile = Paths.get(sampleJarUrl.toURI());
        JkDependencySet dependencies = JkDependencySet.of()
                .andFiles(jarFile);
        JkDependencyResolver dependencyResolver = JkDependencyResolver.of()
                .addRepos(JkRepo.ofMavenCentral());
        JkResolveResult resolveResult = dependencyResolver.resolve(dependencies);
        Assert.assertEquals(1, resolveResult.getDependencyTree().getChildren().size());
    }

    private static Path unzipToDir(String zipName) throws IOException, URISyntaxException {
        final Path dest = Files.createTempDirectory(JkDependencyResolverTest.class.getName());
        final Path zip = Paths.get(JkDependencyResolverTest.class.getResource(zipName).toURI());
        JkPathTree.ofZip(zip).copyTo(dest);
        System.out.println("unzipped in " + dest);
        return dest;
    }

}
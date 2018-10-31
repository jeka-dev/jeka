package org.jerkar.api.depmanagement;

import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.java.project.JkJavaProject;
import org.jerkar.api.system.JkLog;
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
    public void resolveModuleDependencies() throws Exception {
        JkLog.setVerbosity(JkLog.Verbosity.VERBOSE);
        JkLog.registerBasicConsoleHandler();
        JkDependencySet dependencies = JkDependencySet.of()
                .and(JkPopularModules.GUAVA, TestConstants.GUAVA_VERSION);
        JkDependencyResolver dependencyResolver = JkDependencyResolver.of(JkRepo.ofMavenCentral());
        JkResolveResult resolveResult = dependencyResolver.resolve(dependencies);
        resolveResult.assertNoError();
        Assert.assertEquals(1, resolveResult.getDependencyTree().getChildren().size());
    }

    @Test
    public void resolveDependenciesOfTypeProject() throws Exception {
        Path root = unzipToDir("sample-multiproject.zip");
        Path base = root.resolve("base");
        Path core = root.resolve("core");
        JkJavaProject baseProject = JkJavaProject.ofMavenLayout(base);
        JkJavaProject coreProject = JkJavaProject.ofMavenLayout(core);
        baseProject.setDependencies(JkDependencySet.of().and(JkPopularModules.GUAVA, TestConstants.GUAVA_VERSION));
        coreProject.setDependencies(JkDependencySet.of().and(baseProject));
        JkDependencyResolver dependencyResolver = JkDependencyResolver.of(JkRepo.ofMavenCentral().toSet());

        JkResolveResult resolveResult = dependencyResolver.resolve(coreProject.getDependencies());

        Assert.assertEquals(1, resolveResult.getDependencyTree().getChildren().size());
        JkDependencyNode dependencyNode = resolveResult.getDependencyTree().getChildren().get(0);
        Assert.assertFalse(dependencyNode.isModuleNode());
        JkDependencyNode.JkFileNodeInfo nodeInfo = (JkDependencyNode.JkFileNodeInfo) dependencyNode.getNodeInfo();
        Assert.assertEquals(baseProject.getBaseDir(), nodeInfo.computationOrigin().getIdeProjectBaseDir());
    }

    @Test
    public void resolveMixedTypeDependencies() throws Exception {
        URL sampleJarUrl = JkDependencyResolverTest.class.getResource("myArtifactSample.jar");
        Path jarFile = Paths.get(sampleJarUrl.toURI());
        JkDependencySet dependencies = JkDependencySet.of()
                .and(JkPopularModules.GUAVA, TestConstants.GUAVA_VERSION)
                .andFiles(jarFile);
        JkDependencyResolver dependencyResolver = JkDependencyResolver.of(JkRepo.ofMavenCentral());
        JkResolveResult resolveResult = dependencyResolver.resolve(dependencies);
        Assert.assertEquals(2, resolveResult.getDependencyTree().getChildren().size());
        resolveResult.assertNoError();
    }

    @Test
    public void resolveOnlyFilesDependencies() throws Exception {
        URL sampleJarUrl = JkDependencyResolverTest.class.getResource("myArtifactSample.jar");
        Path jarFile = Paths.get(sampleJarUrl.toURI());
        JkDependencySet dependencies = JkDependencySet.of()
                .andFiles(jarFile);
        JkDependencyResolver dependencyResolver = JkDependencyResolver.of(JkRepo.ofMavenCentral());
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
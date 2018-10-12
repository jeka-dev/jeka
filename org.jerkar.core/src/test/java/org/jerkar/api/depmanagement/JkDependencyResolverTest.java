package org.jerkar.api.depmanagement;

import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.java.project.JkJavaProject;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JkDependencyResolverTest {

    @Test
    public void resolveDependenciesOfTypeProject() throws Exception {
        Path root = unzipToDir("sample-multiproject.zip");
        Path base = root.resolve("base");
        Path core = root.resolve("core");
        JkJavaProject baseProject = JkJavaProject.ofMavenLayout(base);
        JkJavaProject coreProject = JkJavaProject.ofMavenLayout(core);
        baseProject.setDependencies(JkDependencySet.of().and(JkPopularModules.GUAVA, "19.0"));
        coreProject.setDependencies(JkDependencySet.of().and(baseProject));
        JkDependencyResolver dependencyResolver = JkDependencyResolver.of(JkRepo.ofMavenCentral().toSet());

        JkResolveResult resolveResult = dependencyResolver.resolve(coreProject.getDependencies());

        Assert.assertEquals(1, resolveResult.getDependencyTree().getChildren().size());
        JkDependencyNode dependencyNode = resolveResult.getDependencyTree().getChildren().get(0);
        Assert.assertFalse(dependencyNode.isModuleNode());
        JkDependencyNode.FileNodeInfo nodeInfo = (JkDependencyNode.FileNodeInfo) dependencyNode.getNodeInfo();
        Assert.assertEquals(baseProject.baseDir(), nodeInfo.computationOrigin().getIdeProjectBaseDir());
    }

    private static Path unzipToDir(String zipName) throws IOException, URISyntaxException {
        final Path dest = Files.createTempDirectory(JkDependencyResolverTest.class.getName());
        final Path zip = Paths.get(JkDependencyResolverTest.class.getResource(zipName).toURI());
        JkPathTree.ofZip(zip).copyTo(dest);
        System.out.println("unzipped in " + dest);
        return dest;
    }

}
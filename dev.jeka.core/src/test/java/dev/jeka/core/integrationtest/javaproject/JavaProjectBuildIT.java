package dev.jeka.core.integrationtest.javaproject;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import dev.jeka.core.api.depmanagement.resolution.JkResolvedDependencyNode;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.project.JkJavaProject;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JavaProjectBuildIT {

    @Test
    public void resolve_dependencyOfTypeProject_resultWithProjectIdeDir() throws Exception {
        Path root = unzipToDir("sample-multiproject.zip");

        JkJavaProject baseProject = JkJavaProject.of().simpleFacade()
                .setBaseDir(root.resolve("base"))
                .addCompileDependencies(JkDependencySet.of()
                        .and("com.google.guava:guava:23.0")).getProject();

        JkJavaProject coreProject = JkJavaProject.of().simpleFacade()
                .setBaseDir(root.resolve("core"))
                .addCompileDependencies(JkDependencySet.of(baseProject.toDependency())).getProject();

        JkResolveResult resolveResult = coreProject.getConstruction().getCompilation().resolveDependencies();

        Assert.assertEquals(2, resolveResult.getDependencyTree().getChildren().size()); // base dir and guava
        JkResolvedDependencyNode dependencyNode = resolveResult.getDependencyTree().getChildren().get(0);
        Assert.assertFalse(dependencyNode.isModuleNode());
        JkResolvedDependencyNode.JkFileNodeInfo nodeInfo = (JkResolvedDependencyNode.JkFileNodeInfo) dependencyNode.getNodeInfo();
        Assert.assertEquals(baseProject.getBaseDir(), nodeInfo.computationOrigin().getIdeProjectDir());
    }

    private static Path unzipToDir(String zipName) throws IOException, URISyntaxException {
        final Path dest = Files.createTempDirectory(JavaProjectBuildIT.class.getName());
        final Path zip = Paths.get(JavaProjectBuildIT.class.getResource(zipName).toURI());
        JkPathTree.ofZip(zip).copyTo(dest);
        System.out.println("unzipped in " + dest);
        return dest;
    }

}

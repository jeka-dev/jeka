package dev.jeka.core.integrationtest.javaproject;

import dev.jeka.core.api.depmanagement.JkDependency;
import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkDependencySet.Hint;
import dev.jeka.core.api.depmanagement.JkTransitivity;
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
import java.util.List;

public class JavaProjectBuildIT {

    @Test
    public void resolve_dependencyOfTypeProject_resultWithProjectIdeDir() throws Exception {
        Path root = unzipToDir("sample-multiproject.zip");

        JkJavaProject baseProject = JkJavaProject.of().simpleFacade()
                .setBaseDir(root.resolve("base"))
                .setCompileDependencies(deps -> deps
                        .and("com.google.guava:guava:23.0")).getProject();

        JkJavaProject coreProject = JkJavaProject.of().simpleFacade()
                .setBaseDir(root.resolve("core"))
                .setCompileDependencies(deps -> deps.and(baseProject.toDependency())).getProject();

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

    @Test
    public void getTestDependencies_ok() {
        JkJavaProject project = JkJavaProject.of().simpleFacade()
                .setCompileDependencies(deps -> deps
                        .and("com.google.guava:guava:23.0", JkTransitivity.NONE)
                        .and("javax.servlet:javax.servlet-api:4.0.1"))
                .setRuntimeDependencies(deps -> deps
                        .and("org.postgresql:postgresql:42.2.19")
                        .withTransitivity("com.google.guava:guava", JkTransitivity.RUNTIME)
                        .minus("javax.servlet:javax.servlet-api"))
                .setTestDependencies(deps -> deps
                        .and(Hint.first(), "org.mockito:mockito-core:2.10.0")
                )
                .setPublishedModuleId("my:project").setPublishedVersion("MyVersion")
                .getProject();
        JkDependencySet testDependencies = project.getConstruction().getTesting().getCompilation().getDependencies();
        System.out.println(project.getInfo());
        Assert.assertEquals(JkTransitivity.RUNTIME, testDependencies.get("com.google.guava:guava").getTransitivity());
        Assert.assertNotNull(testDependencies.get("javax.servlet:javax.servlet-api"));
        Assert.assertEquals("org.mockito:mockito-core", testDependencies.getModuleDependencies().get(0)
                .getModuleId().toString());
    }

    @Test
    public void publish_maven_ok() {
        JkJavaProject project = JkJavaProject.of().simpleFacade()
                .setCompileDependencies(deps -> deps
                        .and("com.google.guava:guava:23.0", JkTransitivity.NONE)
                        .and("javax.servlet:javax.servlet-api:4.0.1"))
                .setRuntimeDependencies(deps -> deps
                        .and("org.postgresql:postgresql:42.2.19")
                        .withTransitivity("com.google.guava:guava", JkTransitivity.RUNTIME)
                        .minus("javax.servlet:javax.servlet-api"))
                .setTestDependencies(deps -> deps
                        .and("org.mockito:mockito-core:2.10.0")
                )
                .setPublishedModuleId("my:project").setPublishedVersion("MyVersion")
                .getProject();
        project.getConstruction().getDependencyResolver().resolve(project.getConstruction().getRuntimeDependencies());
        List<JkDependency> dependencies = project.getPublication().getMavenPublication()
                .getDependencies().getDependencies();
        System.out.println(project.getInfo());
    }

}

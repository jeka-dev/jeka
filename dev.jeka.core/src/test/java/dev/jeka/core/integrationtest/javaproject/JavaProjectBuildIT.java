package dev.jeka.core.integrationtest.javaproject;

import dev.jeka.core.api.depmanagement.JkTransitivity;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import dev.jeka.core.api.depmanagement.resolution.JkResolvedDependencyNode;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.project.JkProject;
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

        JkProject baseProject = JkProject.of().simpleFacade()
                .setBaseDir(root.resolve("base"))
                .setCompileDependencies(deps -> deps
                        .and("com.google.guava:guava:23.0")).getProject();

        JkProject coreProject = JkProject.of().simpleFacade()
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
    public void publish_maven_ok() throws IOException, URISyntaxException {
        Path root = unzipToDir("sample-multiproject.zip");
        JkProject project = JkProject.of().simpleFacade()
                .setBaseDir(root.resolve("base"))
                .setCompileDependencies(deps -> deps
                        .and("com.google.guava:guava:23.0")
                        .and("javax.servlet:javax.servlet-api:4.0.1"))
                .setRuntimeDependencies(deps -> deps
                        .and("org.postgresql:postgresql:42.2.19")
                        .withTransitivity("com.google.guava:guava", JkTransitivity.RUNTIME)
                        .minus("javax.servlet:javax.servlet-api"))
                .setTestDependencies(deps -> deps
                        .and("org.mockito:mockito-core:2.10.0")
                )
                .setPublishedMavenModuleId("my:project").setPublishedMavenVersion("MyVersion-snapshot")
                .setPublishedMavenVersion("1-SNAPSHOT")
                .getProject();
        project.getPublication().getArtifactProducer().makeAllArtifacts();
        project.getPublication().getMaven().publishLocal();
        System.out.println(project.getInfo());
        Assert.assertEquals(JkTransitivity.COMPILE, project.getPublication().getMaven().getDependencies()
                .get("com.google.guava:guava").getTransitivity());

    }

    @Test
    public void publish_ivy_ok() throws IOException, URISyntaxException {
        Path root = unzipToDir("sample-multiproject.zip");
        JkProject project = JkProject.of().simpleFacade()
                .setBaseDir(root.resolve("base"))
                .setCompileDependencies(deps -> deps
                        .and("com.google.guava:guava:23.0")
                        .and("javax.servlet:javax.servlet-api:4.0.1"))
                .setRuntimeDependencies(deps -> deps
                        .and("org.postgresql:postgresql:42.2.19")
                        .withTransitivity("com.google.guava:guava", JkTransitivity.RUNTIME)
                        .minus("javax.servlet:javax.servlet-api"))
                .setTestDependencies(deps -> deps
                        .and("org.mockito:mockito-core:2.10.0")
                ).getProject();
        project.getPublication().getIvy()
                .setModuleId("my:module")
                .setVersion("0.1");
        project.getPublication().getArtifactProducer().makeAllArtifacts();
        project.getPublication().getIvy().publishLocal();
        System.out.println(project.getInfo());


    }

}

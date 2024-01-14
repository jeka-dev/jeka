package dev.jeka.core.api.project;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.JkDependencySet.Hint;
import dev.jeka.core.api.depmanagement.publication.JkIvyPublication;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkZipTree;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class JkProjectTest {

    @Test
    public void getTestDependencies_containsCompileDependencies() {
        JkProject javaProject = JkProject.of()
                .flatFacade()
                .customizeCompileDeps(deps -> deps.and("a:a"))
                .customizeTestDeps(deps -> deps.and("b:b"))
                .getProject();
        JkDependencySet compileDeps = javaProject
                .compilation.getDependencies();
        JkDependencySet testCompileDeps = javaProject.testing
                .compilation.getDependencies();
        Assert.assertEquals(1, compileDeps.getEntries().stream()
                .filter(JkCoordinateDependency.class::isInstance)
                .collect(Collectors.toList())
                .size());
        Assert.assertNotNull(compileDeps.get("a:a"));
        Assert.assertEquals(2, testCompileDeps.getEntries().stream()
                .filter(JkCoordinateDependency.class::isInstance)
                .collect(Collectors.toList())
                .size());
        Assert.assertNotNull(testCompileDeps.get("a:a"));
        Assert.assertNotNull(testCompileDeps.get("b:b"));
    }

    public void addDependencies() {
        JkProject javaProject = JkProject.of()
                .flatFacade()
                .addCompileDeps("a:a", "a:a1")
                .addCompileOnlyDeps("a:a2")
                .addRuntimeDeps("c:c")
                .addTestDeps("b:b")
                .getProject();
        JkDependencySet compileDeps = javaProject
                .compilation.getDependencies();
        JkDependencySet testCompileDeps = javaProject.testing
                .compilation.getDependencies();
        JkDependencySet runtimeDeps = javaProject.packaging.getRuntimeDependencies();
        Assert.assertEquals(3, compileDeps.getEntries().stream()
                .filter(JkCoordinateDependency.class::isInstance)
                .collect(Collectors.toList())
                .size());
        Assert.assertEquals(3, runtimeDeps.getEntries().stream()
                .filter(JkCoordinateDependency.class::isInstance)
                .collect(Collectors.toList())
                .size());
        Assert.assertEquals(5, testCompileDeps.getEntries().stream()
                .filter(JkCoordinateDependency.class::isInstance)
                .collect(Collectors.toList())
                .size());
    }

    @Test
    public void getTestDependencies_usingSetTestDependency_ok() {
        JkProject project = JkProject.of().flatFacade()
                .customizeCompileDeps(deps -> deps
                        .and("com.google.guava:guava:23.0", JkTransitivity.NONE)
                        .and("javax.servlet:javax.servlet-api:4.0.1"))
                .customizeRuntimeDeps(deps -> deps
                        .and("org.postgresql:postgresql:42.2.19")
                        .withTransitivity("com.google.guava:guava", JkTransitivity.RUNTIME)
                        .minus("javax.servlet:javax.servlet-api"))
                .customizeTestDeps(deps -> deps
                        .and(Hint.first(), "org.mockito:mockito-core:2.10.0")
                )
                .setModuleId("my:project").setVersion("MyVersion")
                .getProject();
        JkDependencySet testDependencies = project.testing.compilation.getDependencies();
        System.out.println(project.getInfo());
        Assert.assertEquals(JkTransitivity.RUNTIME, testDependencies.get("com.google.guava:guava").getTransitivity());
        Assert.assertNotNull(testDependencies.get("javax.servlet:javax.servlet-api"));
        Assert.assertEquals("org.mockito:mockito-core", testDependencies.getCoordinateDependencies().get(0)
                .getCoordinate().getModuleId().toString());
    }

    @Test
    public void addVersionProviderOnCompile_testAndRuntimeHaveVersionProvider() {
        JkVersionProvider versionProvider = JkVersionProvider.of()
                .and("javax.servlet:javax.servlet-api", "4.0.1");
        JkProject project = JkProject.of().flatFacade()
                .customizeCompileDeps(deps -> deps
                        .andVersionProvider(versionProvider)
                        .and("javax.servlet:javax.servlet-api")
                ).getProject();
        JkDependencySet testDeps = project.testing.compilation.getDependencies();
        Assert.assertEquals("4.0.1",
                testDeps.getVersionProvider().getVersionOf("javax.servlet:javax.servlet-api"));
    }

    @Test
    public void getTestDependencies_usingAddTestDependency_ok() {
        JkProject project = JkProject.of().flatFacade()
                .customizeCompileDeps(deps -> deps
                        .and("com.google.guava:guava:23.0", JkTransitivity.NONE)
                        .and("javax.servlet:javax.servlet-api:4.0.1"))
                .customizeRuntimeDeps(deps -> deps
                        .and("org.postgresql:postgresql:42.2.19")
                        .withTransitivity("com.google.guava:guava", JkTransitivity.RUNTIME)
                        .minus("javax.servlet:javax.servlet-api"))
                .customizeTestDeps(deps -> deps
                        .and(Hint.first(), "io.rest-assured:rest-assured:4.3.3")
                        .and(Hint.first(), "org.mockito:mockito-core:2.10.0")
                )
                .setModuleId("my:project").setVersion("MyVersion")
                .getProject();
        JkDependencySet testDependencies = project.testing.compilation.getDependencies();
        System.out.println(project.getInfo());
        Assert.assertEquals(JkTransitivity.RUNTIME, testDependencies.get("com.google.guava:guava").getTransitivity());
        Assert.assertNotNull(testDependencies.get("javax.servlet:javax.servlet-api"));
        Assert.assertEquals("org.mockito:mockito-core", testDependencies.getCoordinateDependencies().get(0)
                .getCoordinate().getModuleId().toString());
        Assert.assertEquals("io.rest-assured:rest-assured", testDependencies.getCoordinateDependencies().get(1)
                .getCoordinate().getModuleId().toString());
    }

    @Test
    public void getPublishMavenDependencies_ok() {
        JkProject project = JkProject.of().flatFacade()
                .customizeCompileDeps(deps -> deps
                        .and("com.google.guava:guava:23.0", JkTransitivity.NONE)
                        .and("javax.servlet:javax.servlet-api:4.0.1"))
                .customizeRuntimeDeps(deps -> deps
                        .and("org.postgresql:postgresql:42.2.19")
                        .withTransitivity("com.google.guava:guava", JkTransitivity.RUNTIME)
                        .minus("javax.servlet:javax.servlet-api"))
                .customizeTestDeps(deps -> deps
                        .and(Hint.first(), "org.mockito:mockito-core:2.10.0")
                        .and(Hint.first(), "io.rest-assured:rest-assured:4.3.3")
                )
                .setModuleId("my:project").setVersion("MyVersion")
                .customizePublishedDeps(deps -> deps.minus("org.postgresql:postgresql"))
                .getProject();
        JkDependencySet publishDeps = project.mavenPublication.getDependencies();
        publishDeps.getEntries().forEach(System.out::println);
        Assert.assertEquals(JkTransitivity.COMPILE, publishDeps.get("javax.servlet:javax.servlet-api").getTransitivity());
    }

    @Test
    public void getPublishIvyDependencies_ok() {
        JkProject project = JkProject.of().flatFacade()
                .customizeCompileDeps(deps -> deps
                        .and("com.google.guava:guava:23.0", JkTransitivity.NONE)
                        .and("javax.servlet:javax.servlet-api:4.0.1"))
                .customizeRuntimeDeps(deps -> deps
                        .and("org.postgresql:postgresql:42.2.19")
                        .withTransitivity("com.google.guava:guava", JkTransitivity.RUNTIME)
                        .minus("javax.servlet:javax.servlet-api"))
                .customizeCompileDeps(deps -> deps
                        .and(Hint.first(), "org.mockito:mockito-core:2.10.0")
                        .and(Hint.first(), "io.rest-assured:rest-assured:4.3.3")
                ).getProject();
        JkIvyPublication ivyPublication = project.createIvyPublication()
                .setModuleId("my:module")
                .setVersion("0.1");
        System.out.println(project.compilation.getDependencies());
        JkQualifiedDependencySet publishDeps = ivyPublication.getDependencies();
        publishDeps.getEntries().forEach(System.out::println);
    }

    @Test
    public void runDisplayDependencies() {
        //JkLog.setDecorator(JkLog.Style.INDENT);
        JkProject project = JkProject.of().flatFacade()
                .customizeCompileDeps(deps -> deps
                        .and("com.google.guava:guava:23.0", JkTransitivity.NONE)
                        .and("javax.servlet:javax.servlet-api:4.0.1"))
                .customizeRuntimeDeps(deps -> deps
                        .and("org.postgresql:postgresql:42.2.19")
                        .withTransitivity("com.google.guava:guava", JkTransitivity.RUNTIME)
                        .minus("javax.servlet:javax.servlet-api"))
                .customizeCompileDeps(deps -> deps
                        .and(Hint.first(), "org.mockito:mockito-core:2.10.0")
                        .and(Hint.first(), "io.rest-assured:rest-assured:4.3.3")
                ).getProject();
        project.displayDependencyTree();
    }

    @Test
    public void addCompileOnlyDependency_ok() {
        JkProject project = JkProject.of();
        project.flatFacade()
                .addCompileOnlyDeps(
                        "org.projectlombok:lombok:1.18.30"
                );
        Assert.assertTrue(project.packaging.getRuntimeDependencies().getEntries().isEmpty());
    }

    @Test
    public void makeAllArtifacts() throws Exception {
        final Path top = unzipToDir("sample-multi-scriptless.zip");

        Path base = top.resolve("base");
        JkProject baseProject = JkProject.of();
        baseProject.setBaseDir(base).flatFacade()
                .customizeCompileDeps(deps -> deps.and(JkPopularLibs.APACHE_HTTP_CLIENT.toCoordinate("4.5.14")))
                .getProject()
                    .compilation
                        .layout
                            .emptySources().addSource("src")
                            .emptyResources().addResource("res")
                            .mixResourcesAndSources();
        baseProject.pack();

        final Path core = top.resolve("core");
        final JkProject coreProject = JkProject.of();
        coreProject
                .setBaseDir(core)
                    .compilation
                        .customizeDependencies(deps -> deps
                            .and(baseProject.toDependency())
                        )
                        .layout
                            .setSourceSimpleStyle(JkCompileLayout.Concern.PROD);

        //Desktop.getDesktop().open(core.toFile());
        coreProject.pack();

        final Path desktop = top.resolve("desktop");
        final JkProject desktopProject = JkProject.of();
        desktopProject
                .setBaseDir(desktop)
                .compilation
                    .customizeDependencies(deps -> deps
                            .and(coreProject.toDependency()));
        desktopProject
                .compilation
                    .layout
                    .setSourceSimpleStyle(JkCompileLayout.Concern.PROD);
        //Desktop.getDesktop().open(desktop.toFile());
        //desktopProject.getArtifactProducer().makeAllArtifacts();

        // Desktop.getDesktop().open(desktop);
        JkPathTree.of(top).deleteRoot();
    }

    private static Path unzipToDir(String zipName) throws IOException, URISyntaxException {
        final Path dest = Files.createTempDirectory(JkProjectTest.class.getName());
        final Path zip = Paths.get(JkProjectTest.class.getResource(zipName).toURI());
        JkZipTree.of(zip).copyTo(dest);
        System.out.println("unzipped in " + dest);
        return dest;
    }

    @Test
    public void getRuntimeDependencies_usingDependenciesTxt_ok() {
        JkProject project = JkProject.of()
                    .setIncludeTextAndLocalDependencies(true);
        URL dependencyTxtUrl = JkProjectTest.class.getResource("simple-dependencies-simple.txt");
        project.setDependencyTxtUrl(dependencyTxtUrl);
        JkDependencySet runtimeDependencies = project.packaging.getRuntimeDependencies();
        JkCoordinateDependency lombokDep = runtimeDependencies.getMatching(JkCoordinateDependency.of("org.projectlombok:lombok"));
        runtimeDependencies.getEntries().forEach(System.out::println);
        Assert.assertNull(lombokDep);  // expect lombok not included


    }
}

package dev.jeka.core.api.project;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.JkDependencySet.Hint;
import dev.jeka.core.api.depmanagement.publication.JkIvyPublication;
import dev.jeka.core.api.depmanagement.publication.JkMavenPublication;
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

public class JkProjectTest {

    @Test
    public void getTestDependencies_containsCompileDependencies() {
        JkProject javaProject = JkProject.of();
        javaProject.compilation.dependencies.add(("a:a"));
        javaProject.testing.compilation.dependencies.add("b:b");
        JkDependencySet compileDeps = javaProject.compilation.dependencies.get();
        JkDependencySet testCompileDeps = javaProject.testing.compilation.dependencies.get();
        Assert.assertEquals(1, compileDeps.getEntries().stream()
                .filter(JkCoordinateDependency.class::isInstance).count());
        Assert.assertNotNull(compileDeps.get("a:a"));
        Assert.assertEquals(2, testCompileDeps.getEntries().stream()
                .filter(JkCoordinateDependency.class::isInstance).count());
        Assert.assertNotNull(testCompileDeps.get("a:a"));
        Assert.assertNotNull(testCompileDeps.get("b:b"));
    }

    @Test
    public void addDependencies() {
        JkProject project = JkProject.of();
        project.setIncludeTextAndLocalDependencies(false);

        project.compilation.dependencies.add("a:a").add("a:a1");
        project.flatFacade.addCompileOnlyDeps("a:a2");

        project.flatFacade.dependencies.runtime.add("c:c");

        project.flatFacade.dependencies.test.add("b:b");

        Assert.assertEquals(3, project.compilation.dependencies.get().getEntries().size());
        Assert.assertEquals(3, project.packaging.runtimeDependencies.get().getEntries().size());
        Assert.assertEquals(5, project.testing.compilation.dependencies.get().getEntries().size());
    }

    @Test
    public void getTestDependencies_usingSetTestDependency_ok() {
        JkProject project = JkProject.of();
        project.compilation.dependencies
                        .add("com.google.guava:guava:23.0", JkTransitivity.NONE)
                        .add("javax.servlet:javax.servlet-api:4.0.1");
        project.flatFacade.dependencies.runtime
                        .add("org.postgresql:postgresql:42.2.19")
                        .modify(deps -> deps.withTransitivity("com.google.guava:guava", JkTransitivity.RUNTIME))
                        .remove("javax.servlet:javax.servlet-api");
        project.flatFacade.dependencies.test
                        .modify(deps -> deps.and(Hint.first(), "org.mockito:mockito-core:2.10.0"));
        project.setModuleId("my:project").setVersion("MyVersion");
        JkDependencySet testDependencies = project.testing.compilation.dependencies.get();
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
        JkProject project = JkProject.of();
        project.flatFacade.dependencies.runtime
                        .addVersionProvider(versionProvider)
                        .add("javax.servlet:javax.servlet-api");
        JkDependencySet testDeps = project.testing.compilation.dependencies.get();
        Assert.assertEquals("4.0.1",
                testDeps.getVersionProvider().getVersionOf("javax.servlet:javax.servlet-api"));
    }

    @Test
    public void getTestDependencies_usingAddTestDependency_ok() {
        JkProject project = JkProject.of();
        project.compilation.dependencies
                    .add("com.google.guava:guava:23.0", JkTransitivity.NONE)
                    .add("javax.servlet:javax.servlet-api:4.0.1");
        project.packaging.runtimeDependencies
                    .add("org.postgresql:postgresql:42.2.19")
                    .modify(deps -> deps.withTransitivity("com.google.guava:guava", JkTransitivity.RUNTIME))
                    .remove("javax.servlet:javax.servlet-api");
        project.testing.compilation.dependencies
                    .modify(deps -> deps.and(Hint.first(), "io.rest-assured:rest-assured:4.3.3"))
                    .modify(deps -> deps.and(Hint.first(), "org.mockito:mockito-core:2.10.0"));
        project.setModuleId("my:project").setVersion("MyVersion");

        JkDependencySet testDependencies = project.testing.compilation.dependencies.get();
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
        JkProject project = JkProject.of();
        project.compilation.dependencies
                        .add("com.google.guava:guava:23.0", JkTransitivity.NONE)
                        .add("javax.servlet:javax.servlet-api:4.0.1");
        project.packaging.runtimeDependencies
                        .add("org.postgresql:postgresql:42.2.19")
                        .modify(deps -> deps.withTransitivity("com.google.guava:guava", JkTransitivity.RUNTIME))
                        .remove("javax.servlet:javax.servlet-api");
        project.testing.compilation.dependencies
                        .modify(deps -> deps.and(Hint.first(), "org.mockito:mockito-core:2.10.0"))
                        .modify(deps -> deps.and(Hint.first(), "io.rest-assured:rest-assured:4.3.3"));
        project.setModuleId("my:project").setVersion("MyVersion");

        JkMavenPublication mavenPublication = JkMavenPublication.of(project.asBuildable());
        mavenPublication.customizeDependencies(deps -> deps.minus("org.postgresql:postgresql"));
        JkDependencySet publishDeps = mavenPublication.getDependencies();
        publishDeps.getEntries().forEach(System.out::println);
        Assert.assertEquals(JkTransitivity.COMPILE, publishDeps.get("javax.servlet:javax.servlet-api").getTransitivity());
    }

    @Test
    public void getPublishIvyDependencies_ok() {
        JkProject project = JkProject.of();
        project.compilation.dependencies
                .add("com.google.guava:guava:23.0", JkTransitivity.NONE)
                .add("javax.servlet:javax.servlet-api:4.0.1");
        project.packaging.runtimeDependencies
                .add("org.postgresql:postgresql:42.2.19")
                .modify(deps -> deps.withTransitivity("com.google.guava:guava", JkTransitivity.RUNTIME))
                .remove("javax.servlet:javax.servlet-api");
        project.compilation.dependencies.modify(deps -> deps
                .and(Hint.first(), "org.mockito:mockito-core:2.10.0")
                .and(Hint.first(), "io.rest-assured:rest-assured:4.3.3")
        );

        JkIvyPublication ivyPublication = JkProjectPublications.ivyPublication(project)
                .setModuleId("my:module")
                .setVersion("0.1");
        System.out.println(project.compilation.dependencies.get());
        JkQualifiedDependencySet publishDeps = ivyPublication.getDependencies();
        publishDeps.getEntries().forEach(System.out::println);
    }

    @Test
    public void runDisplayDependencies() {
        //JkLog.setDecorator(JkLog.Style.INDENT);
        JkProject project = JkProject.of();
        project.compilation.dependencies
                .add("com.google.guava:guava:23.0", JkTransitivity.NONE)
                .add("javax.servlet:javax.servlet-api:4.0.1");
        project.packaging.runtimeDependencies
                .add("org.postgresql:postgresql:42.2.19")
                .modify(deps -> deps.withTransitivity("com.google.guava:guava", JkTransitivity.RUNTIME))
                .remove("javax.servlet:javax.servlet-api");
        project.compilation.dependencies.modify(deps -> deps
                .and(Hint.first(), "org.mockito:mockito-core:2.10.0")
                .and(Hint.first(), "io.rest-assured:rest-assured:4.3.3"));
        project.displayDependencyTree();
    }

    @Test
    public void addCompileOnlyDependency_ok() {
        JkProject project = JkProject.of();
        project.flatFacade.addCompileOnlyDeps("org.projectlombok:lombok:1.18.30");
        Assert.assertTrue(project.packaging.runtimeDependencies.get().getEntries().isEmpty());
    }

    @Test
    public void makeAllArtifacts() throws Exception {
        final Path top = unzipToDir("sample-multi-scriptless.zip");

        Path base = top.resolve("base");
        JkProject baseProject = JkProject.of().setBaseDir(base);
        baseProject.flatFacade.dependencies.compile
                    .add(JkPopularLibs.APACHE_HTTP_CLIENT.toCoordinate("4.5.14"));
        baseProject.compilation.layout
                .emptySources().addSource("src")
                .emptyResources().addResource("res")
                .mixResourcesAndSources();
        baseProject.pack();

        final Path core = top.resolve("core");
        final JkProject coreProject = JkProject.of();
        coreProject.setBaseDir(core);
        coreProject.compilation.dependencies
                .add(baseProject.toDependency());
        coreProject.compilation.layout
                .setSourceSimpleStyle(JkCompileLayout.Concern.PROD);

        //Desktop.getDesktop().open(core.toFile());
        coreProject.pack();

        final Path desktop = top.resolve("desktop");
        final JkProject desktopProject = JkProject.of();
        desktopProject
                .setBaseDir(desktop);
        desktopProject.compilation.dependencies
                    .add(coreProject.toDependency());
        desktopProject.compilation.layout
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
        JkDependencySet runtimeDependencies = project.packaging.runtimeDependencies.get();
        JkCoordinateDependency lombokDep = runtimeDependencies.getMatching(JkCoordinateDependency.of("org.projectlombok:lombok"));
        runtimeDependencies.getEntries().forEach(System.out::println);
        Assert.assertNull(lombokDep);  // expect lombok not included


    }
}

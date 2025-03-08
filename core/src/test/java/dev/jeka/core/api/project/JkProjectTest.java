package dev.jeka.core.api.project;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.JkDependencySet.Hint;
import dev.jeka.core.api.depmanagement.publication.JkIvyPublication;
import dev.jeka.core.api.depmanagement.publication.JkMavenPublication;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkZipTree;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JkProjectTest {

    @Test
    void getTestDependencies_containsCompileDependencies() {
        JkProject javaProject = JkProject.of();
        javaProject.compilation.dependencies.add(("a:a"));
        javaProject.testing.compilation.dependencies.add("b:b");
        JkDependencySet compileDeps = javaProject.compilation.dependencies.get();
        JkDependencySet testCompileDeps = javaProject.testing.compilation.dependencies.get();
        assertEquals(1, compileDeps.getEntries().stream()
                .filter(JkCoordinateDependency.class::isInstance).count());
        assertNotNull(compileDeps.get("a:a"));
        assertEquals(2, testCompileDeps.getEntries().stream()
                .filter(JkCoordinateDependency.class::isInstance).count());
        assertNotNull(testCompileDeps.get("a:a"));
        assertNotNull(testCompileDeps.get("b:b"));
    }

    @Test
    void addDependencies() {
        JkProject project = JkProject.of();
        project.setIncludeTextAndLocalDependencies(false);

        project.compilation.dependencies.add("a:a").add("a:a1");
        project.flatFacade.addCompileOnlyDeps("a:a2");

        project.flatFacade.dependencies.runtime.add("c:c");

        project.flatFacade.dependencies.test.add("b:b");

        assertEquals(3, project.compilation.dependencies.get().getEntries().size());
        assertEquals(3, project.packaging.runtimeDependencies.get().getEntries().size());
        assertEquals(5, project.testing.compilation.dependencies.get().getEntries().size());
    }

    @Test
    void getTestDependencies_usingSetTestDependency_ok() {
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
        assertEquals(JkTransitivity.RUNTIME, testDependencies.get("com.google.guava:guava").getTransitivity());
        assertNotNull(testDependencies.get("javax.servlet:javax.servlet-api"));
        assertEquals("org.mockito:mockito-core", testDependencies.getCoordinateDependencies().get(0)
                .getCoordinate().getModuleId().toString());
    }

    @Test
    void addVersionProviderOnCompile_testAndRuntimeHaveVersionProvider() {
        JkVersionProvider versionProvider = JkVersionProvider.of()
                .and("javax.servlet:javax.servlet-api", "4.0.1");
        JkProject project = JkProject.of();
        project.flatFacade.dependencies.runtime
                        .addVersionProvider(versionProvider)
                        .add("javax.servlet:javax.servlet-api");
        JkDependencySet testDeps = project.testing.compilation.dependencies.get();
        assertEquals("4.0.1",
                testDeps.getVersionProvider().getVersionOf("javax.servlet:javax.servlet-api"));
    }

    @Test
    void getTestDependencies_usingAddTestDependency_ok() {
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
        assertEquals(JkTransitivity.RUNTIME, testDependencies.get("com.google.guava:guava").getTransitivity());
        assertNotNull(testDependencies.get("javax.servlet:javax.servlet-api"));
        assertEquals("org.mockito:mockito-core", testDependencies.getCoordinateDependencies().get(0)
                .getCoordinate().getModuleId().toString());
        assertEquals("io.rest-assured:rest-assured", testDependencies.getCoordinateDependencies().get(1)
                .getCoordinate().getModuleId().toString());
    }

    @Test
    void getPublishMavenDependencies_ok() {
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
        assertEquals(JkTransitivity.COMPILE, publishDeps.get("javax.servlet:javax.servlet-api").getTransitivity());
    }

    @Test
    void getPublishIvyDependencies_ok() {
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
    void runDisplayDependencies() {
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
    void addCompileOnlyDependency_ok() {
        JkProject project = JkProject.of();
        project.flatFacade.addCompileOnlyDeps("org.projectlombok:lombok:1.18.30");
        Assertions.assertTrue(project.packaging.runtimeDependencies.get().getEntries().isEmpty());
    }

    @Test
    void makeAllArtifacts() throws Exception {
        final Path top = unzipToDir();

        Path base = top.resolve("base");
        JkProject baseProject = JkProject.of().setBaseDir(base);
        baseProject.flatFacade.dependencies.compile
                    .add(JkPopularLibs.APACHE_HTTP_CLIENT.toCoordinate("4.5.14"));
        baseProject.compilation.layout
                .emptySources().addSources("src")
                .setEmptyResources().addResource("res")
                .setMixResourcesAndSources();
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

    private static Path unzipToDir() throws IOException, URISyntaxException {
        final Path dest = Files.createTempDirectory(JkProjectTest.class.getName());
        final Path zip = Paths.get(JkProjectTest.class.getResource("sample-multi-scriptless.zip").toURI());
        JkZipTree.of(zip).copyTo(dest);
        System.out.println("unzipped in " + dest);
        return dest;
    }

    @Test
    void getRuntimeDependencies_usingDependenciesTxt_ok() {
        JkProject project = JkProject.of()
                    .setIncludeTextAndLocalDependencies(true);
        URL dependencyTxtUrl = JkProjectTest.class.getResource("simple-dependencies-simple.txt");
        project.setDependencyTxtUrl(dependencyTxtUrl);
        JkDependencySet runtimeDependencies = project.packaging.runtimeDependencies.get();
        JkCoordinateDependency lombokDep = runtimeDependencies.getMatching(JkCoordinateDependency.of("org.projectlombok:lombok"));
        runtimeDependencies.getEntries().forEach(System.out::println);
        Assertions.assertNull(lombokDep);  // expect lombok not included
    }

}

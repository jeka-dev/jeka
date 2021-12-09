package dev.jeka.core.api.project;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.JkDependencySet.Hint;
import dev.jeka.core.api.file.JkPathTree;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JkProjectTest {

    @Test
    public void getTestDependencies_containsCompileDependencies() {
        JkProject javaProject = JkProject.of()
                .simpleFacade()
                .configureCompileDeps(deps -> deps.and("a:a"))
                .configureTestDeps(deps -> deps.and("b:b"))
                .getProject();
        JkDependencySet compileDeps = javaProject.getConstruction()
                .getCompilation().getDependencies();
        JkDependencySet testCompileDeps = javaProject.getConstruction().getTesting()
                .getCompilation().getDependencies();
        Assert.assertEquals(1, compileDeps.getEntries().size());
        Assert.assertNotNull(compileDeps.get("a:a"));
        Assert.assertEquals(2, testCompileDeps.getEntries().size());
        Assert.assertNotNull(testCompileDeps.get("a:a"));
        Assert.assertNotNull(testCompileDeps.get("b:b"));
    }

    @Test
    public void getTestDependencies_usingSetTestDependency_ok() {
        JkProject project = JkProject.of().simpleFacade()
                .configureCompileDeps(deps -> deps
                        .and("com.google.guava:guava:23.0", JkTransitivity.NONE)
                        .and("javax.servlet:javax.servlet-api:4.0.1"))
                .configureRuntimeDeps(deps -> deps
                        .and("org.postgresql:postgresql:42.2.19")
                        .withTransitivity("com.google.guava:guava", JkTransitivity.RUNTIME)
                        .minus("javax.servlet:javax.servlet-api"))
                .configureTestDeps(deps -> deps
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
    public void addVersionProviderOnCompile_testAndRuntimeHaveVersionProvider() {
        JkVersionProvider versionProvider = JkVersionProvider.of()
                .and("javax.servlet:javax.servlet-api", "4.0.1");
        JkProject project = JkProject.of().simpleFacade()
                .configureCompileDeps(deps -> deps
                        .andVersionProvider(versionProvider)
                        .and("javax.servlet:javax.servlet-api")
                ).getProject();
        JkDependencySet testDeps = project.getConstruction().getTesting().getCompilation().getDependencies();
        Assert.assertEquals("4.0.1",
                testDeps.getVersionProvider().getVersionOf("javax.servlet:javax.servlet-api"));
    }

    @Test
    public void getTestDependencies_usingAddTestDependency_ok() {
        JkProject project = JkProject.of().simpleFacade()
                .configureCompileDeps(deps -> deps
                        .and("com.google.guava:guava:23.0", JkTransitivity.NONE)
                        .and("javax.servlet:javax.servlet-api:4.0.1"))
                .configureRuntimeDeps(deps -> deps
                        .and("org.postgresql:postgresql:42.2.19")
                        .withTransitivity("com.google.guava:guava", JkTransitivity.RUNTIME)
                        .minus("javax.servlet:javax.servlet-api"))
                .addTestDependencies(deps -> deps
                        .and("org.mockito:mockito-core:2.10.0")
                        .and("io.rest-assured:rest-assured:4.3.3")
                )
                .setPublishedModuleId("my:project").setPublishedVersion("MyVersion")
                .getProject();
        JkDependencySet testDependencies = project.getConstruction().getTesting().getCompilation().getDependencies();
        System.out.println(project.getInfo());
        Assert.assertEquals(JkTransitivity.RUNTIME, testDependencies.get("com.google.guava:guava").getTransitivity());
        Assert.assertNotNull(testDependencies.get("javax.servlet:javax.servlet-api"));
        Assert.assertEquals("org.mockito:mockito-core", testDependencies.getModuleDependencies().get(0)
                .getModuleId().toString());
        Assert.assertEquals("io.rest-assured:rest-assured", testDependencies.getModuleDependencies().get(1)
                .getModuleId().toString());
    }

    @Test
    public void getPublishMavenDependencies_ok() {
        JkProject project = JkProject.of().simpleFacade()
                .configureCompileDeps(deps -> deps
                        .and("com.google.guava:guava:23.0", JkTransitivity.NONE)
                        .and("javax.servlet:javax.servlet-api:4.0.1"))
                .configureRuntimeDeps(deps -> deps
                        .and("org.postgresql:postgresql:42.2.19")
                        .withTransitivity("com.google.guava:guava", JkTransitivity.RUNTIME)
                        .minus("javax.servlet:javax.servlet-api"))
                .addTestDependencies(deps -> deps
                        .and("org.mockito:mockito-core:2.10.0")
                        .and("io.rest-assured:rest-assured:4.3.3")
                )
                .setPublishedModuleId("my:project").setPublishedVersion("MyVersion")
                .configurePublishedDependencies(deps -> deps.minus("org.postgresql:postgresql"))
                .getProject();
        JkDependencySet publishDeps = project.getPublication().getMaven().getDependencies();
        publishDeps.getEntries().forEach(System.out::println);
        Assert.assertEquals(JkTransitivity.COMPILE, publishDeps.get("javax.servlet:javax.servlet-api").getTransitivity());
    }

    @Test
    public void getPublishIvyDependencies_ok() {
        JkProject project = JkProject.of().simpleFacade()
                .configureCompileDeps(deps -> deps
                        .and("com.google.guava:guava:23.0", JkTransitivity.NONE)
                        .and("javax.servlet:javax.servlet-api:4.0.1"))
                .configureRuntimeDeps(deps -> deps
                        .and("org.postgresql:postgresql:42.2.19")
                        .withTransitivity("com.google.guava:guava", JkTransitivity.RUNTIME)
                        .minus("javax.servlet:javax.servlet-api"))
                .addTestDependencies(deps -> deps
                        .and("org.mockito:mockito-core:2.10.0")
                        .and("io.rest-assured:rest-assured:4.3.3")
                ).getProject();
        project.getPublication().getIvy()
                .setModuleId("my:module")
                .setVersion("0.1");
        System.out.println(project.getConstruction().getCompilation().getDependencies());
        JkQualifiedDependencySet publishDeps = project.getPublication().getIvy().getDependencies();
        publishDeps.getEntries().forEach(System.out::println);
    }

    @Test
    public void makeAllArtifacts() throws Exception {
        final Path top = unzipToDir("sample-multi-scriptless.zip");

        Path base = top.resolve("base");
        JkProject baseProject = JkProject.of().simpleFacade()
                .setBaseDir(base)
                .configureCompileDeps(deps -> deps.and(JkPopularModules.APACHE_HTTP_CLIENT.version("4.5.6")))
                .getProject().getConstruction()
                .getCompilation()
                .getLayout()
                .emptySources().addSource("src")
                .emptyResources().addResource("res").mixResourcesAndSources().__.__.__;
        baseProject.getArtifactProducer().makeAllArtifacts();

        final Path core = top.resolve("core");
        final JkProject coreProject = JkProject.of()
                .setBaseDir(core)
                .getConstruction()
                    .getCompilation()
                        .configureDependencies(deps -> deps
                            .and(baseProject.toDependency())
                        )
                        .getLayout()
                            .setSourceSimpleStyle(JkCompileLayout.Concern.PROD)
                        .__
                    .__
                .__;
        //Desktop.getDesktop().open(core.toFile());
        coreProject.getArtifactProducer().makeAllArtifacts();

        final Path desktop = top.resolve("desktop");
        final JkProject desktopProject = JkProject.of()
                .setBaseDir(desktop)
                .getConstruction()
                .getCompilation()
                .configureDependencies(deps -> deps
                        .and(coreProject.toDependency())).__
                .getCompilation()
                .getLayout()
                .setSourceSimpleStyle(JkCompileLayout.Concern.PROD).__.__.__;
        //Desktop.getDesktop().open(desktop.toFile());
        //desktopProject.getArtifactProducer().makeAllArtifacts();

        // Desktop.getDesktop().open(desktop);
        JkPathTree.of(top).deleteRoot();
    }

    private static Path unzipToDir(String zipName) throws IOException, URISyntaxException {
        final Path dest = Files.createTempDirectory(JkProjectTest.class.getName());
        final Path zip = Paths.get(JkProjectTest.class.getResource(zipName).toURI());
        JkPathTree.ofZip(zip).copyTo(dest);
        System.out.println("unzipped in " + dest);
        return dest;
    }
}

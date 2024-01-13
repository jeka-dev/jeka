package dev.jeka.core.integrationtest.resolution;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.JkModuleId;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import dev.jeka.core.api.depmanagement.resolution.JkResolvedDependencyNode;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import org.junit.Assert;
import org.junit.Test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.*;

public class DependencySetResolutionIT {

    private static final String SPRINGBOOT_TEST = "org.springframework.boot:spring-boot-test";

    private static final String SPRINGBOOT_TEST_AND_VERSION = SPRINGBOOT_TEST + ":1.5.3.RELEASE";

    // Commons-login is a transitive dependency of springboot-test
    private static final String COMMONS_LOGIN = "commons-logging:commons-logging";

    private static final String COMMONS_LOGIN_102 = "commons-logging:commons-logging:1.0.2";

    @Test
    public void resolve_unspecifiedVersionButPresentInProvider_ok() {
        JkDependencySet deps = JkDependencySet.of()
                .and("com.google.guava:guava")
                .withVersionProvider(JkVersionProvider.of("com.google.guava:guava", "22.0"));
        JkDependencyResolver resolver = JkDependencyResolver.of().addRepos(JkRepo.ofMavenCentral());
        JkResolveResult resolveResult = resolver.resolve(deps);
        JkResolvedDependencyNode tree = resolveResult.getDependencyTree();
        JkResolvedDependencyNode.JkModuleNodeInfo moduleNodeInfo = tree.getChildren().get(0).getModuleInfo();
        assertEquals("22.0", moduleNodeInfo.getDeclaredVersion().getValue());
    }

    @Test
    public void resolve_jerseyServer_ok() {
        JkDependencySet deps = JkDependencySet.of()
                .and(JkCoordinateDependency.of("com.sun.jersey:jersey-server:1.19.4")
                        .withTransitivity(JkTransitivity.NONE));
        JkDependencyResolver resolver = JkDependencyResolver.of()
                .addRepos(JkRepo.ofMavenCentral());
        JkResolveResult resolveResult = resolver.resolve(deps);
        List<JkResolvedDependencyNode> nodes = resolveResult.getDependencyTree().toFlattenList();
        assertEquals(1, nodes.size());
    }

    @Test
    public void resolve_dependencyDeclaredAsNonTransitive_ok() {
        JkDependencySet deps = JkDependencySet.of()
                .and(JkCoordinateDependency.of(SPRINGBOOT_TEST_AND_VERSION).withTransitivity(JkTransitivity.NONE));
        JkDependencyResolver resolver = JkDependencyResolver.of()
                .addRepos(JkRepo.ofMavenCentral());
        JkResolveResult resolveResult = resolver.resolve(deps);
        List<JkResolvedDependencyNode> nodes = resolveResult.getDependencyTree().toFlattenList();
        assertEquals(1, nodes.size());
    }

    @Test
    public void resolve_transitiveDependency_ok() {
        JkDependencySet deps = JkDependencySet.of()
                .and(JkCoordinateDependency.of(SPRINGBOOT_TEST_AND_VERSION));
        JkDependencyResolver resolver = JkDependencyResolver.of().addRepos(JkRepo.ofMavenCentral());
        JkResolveResult resolveResult = resolver.resolve(deps);
        System.out.println(resolveResult.getFiles());
        assertTrue(resolveResult.contains(JkModuleId.of(SPRINGBOOT_TEST)));
        assertTrue(resolveResult.contains(JkModuleId.of(COMMONS_LOGIN)));
    }

    @Test
    public void resolve_transitiveDependencyLocallyExcluded_ok() {
        JkDependencySet deps = JkDependencySet.of()
                .and(JkCoordinateDependency.of(SPRINGBOOT_TEST_AND_VERSION).andExclusion(COMMONS_LOGIN));
        JkDependencyResolver resolver = JkDependencyResolver.of().addRepos(JkRepo.ofMavenCentral());
        JkResolveResult resolveResult = resolver.resolve(deps);
        assertTrue(resolveResult.contains(JkModuleId.of(SPRINGBOOT_TEST)));
        assertFalse(resolveResult.contains(JkModuleId.of(COMMONS_LOGIN)));
    }

    @Test
    public void resolve_transitiveDependencyGloballyExcluded_ok() {
        JkDependencySet deps = JkDependencySet.of()
                .and(JkCoordinateDependency.of(SPRINGBOOT_TEST_AND_VERSION))
                .andGlobalExclusion(COMMONS_LOGIN);
        JkDependencyResolver resolver = JkDependencyResolver.of().addRepos(JkRepo.ofMavenCentral());
        JkResolveResult resolveResult = resolver.resolve(deps);
        assertTrue(resolveResult.contains(JkModuleId.of(SPRINGBOOT_TEST)));
        assertFalse(resolveResult.contains(JkModuleId.of(COMMONS_LOGIN)));
    }

    @Test
    public void resolve_moduleWithMainAndExtraArtifact_bothArtifactsArePresentInResult() {
        //JkLog.setDecorator(JkLog.Style.INDENT);
        //JkLog.setVerbosity(JkLog.Verbosity.QUITE_VERBOSE);
        JkCoordinate lwjgl= JkCoordinate.of("org.lwjgl:lwjgl:3.1.1");
        JkDependencySet deps = JkDependencySet.of()
                .and(lwjgl)
                .and(lwjgl.withClassifier("natives-linux"));
        JkDependencyResolver resolver = JkDependencyResolver.of().addRepos(JkRepo.ofMavenCentral());
        JkResolveResult resolveResult = resolver.resolve(deps);
        JkResolvedDependencyNode treeRoot = resolveResult.getDependencyTree();
        System.out.println(resolveResult.getFiles());
        System.out.println(treeRoot.toStringTree());

        // Even if there is 2 declared dependencies on lwjgl, as it is the same module (with different artifact),
        // it should results in a single node.
        // The classpath order will also place all artifacts of a same module sequentially.
        assertEquals(1, treeRoot.getChildren().size());
        assertEquals(2, treeRoot.getResolvedFiles().size());

        JkResolvedDependencyNode lwjglNode = treeRoot.getChildren().get(0);
        List<Path> lwjglFiles = lwjglNode.getNodeInfo().getFiles();
        System.out.println(lwjglFiles);
        assertEquals(2, lwjglFiles.size());

    }


    @Test
    public void resolve_notExistingModuleId_reportError() {
        JkCoordinate holder = JkCoordinate.of("mygroup:myname:myversion");
        JkDependencySet deps = JkDependencySet.of()
                .and(JkPopularLibs.JAVAX_SERVLET_API.toCoordinate("2.5.3").toString());  // does not exist
        JkDependencyResolver resolver = JkDependencyResolver.of();
        resolver
                .addRepos(JkRepo.ofMavenCentral())
                .setModuleHolder(holder)
                .getDefaultParams().setFailOnDependencyResolutionError(false);
        JkResolveResult resolveResult = resolver.resolve(deps);
        JkResolveResult.JkErrorReport errorReport = resolveResult.getErrorReport();
        System.out.println(errorReport.getModuleProblems());
        assertEquals(1, errorReport.getModuleProblems().size());
    }

    @Test
    public void resolve_sameDependencyAsDirectAndTransitiveWithDistinctVersion_directWin() {
        JkModuleId starterWebModule = JkModuleId.of("org.springframework.boot:spring-boot-starter-web");
        JkModuleId springCoreModule = JkModuleId.of("org.springframework:spring-core");
        String directCoreVersion = "4.3.6.RELEASE";
        JkDependencySet deps = JkDependencySet.of()
                .and(JkCoordinateDependency.of(starterWebModule.toCoordinate("1.5.10.RELEASE")).withTransitivity(JkTransitivity.COMPILE))
                // force a version lower than the transitive above
                .and(JkCoordinateDependency.of(springCoreModule.toCoordinate(directCoreVersion)).withTransitivity(JkTransitivity.COMPILE));
        JkDependencyResolver resolver = JkDependencyResolver.of()
                .addRepos(JkRepo.ofMavenCentral());
        JkResolveResult resolveResult = resolver.resolve(deps);
        JkResolvedDependencyNode tree = resolveResult.getDependencyTree();
        System.out.println(tree.toStringTree());

        JkResolvedDependencyNode bootNode = tree.getChildren().get(0);
        JkResolvedDependencyNode.JkModuleNodeInfo springCoreTransitiveModuleNodeInfo = bootNode.getFirst(springCoreModule).getModuleInfo();
        assertEquals("4.3.14.RELEASE", springCoreTransitiveModuleNodeInfo.getDeclaredVersion().getValue());
        assertEquals(directCoreVersion, springCoreTransitiveModuleNodeInfo.getResolvedVersion().getValue());  // cause evicted

        // As the spring-core projectVersion is declared as direct dependency and the declared projectVersion is exact (not dynamic)
        // then the resolved version should the direct one.
        JkResolvedDependencyNode.JkModuleNodeInfo springCoreDirectModuleNodeInfo = tree.getChildren().get(1).getModuleInfo();
        assertEquals(directCoreVersion, springCoreDirectModuleNodeInfo.getDeclaredVersion().getValue());
        assertEquals(directCoreVersion, springCoreDirectModuleNodeInfo.getResolvedVersion().getValue());
    }

    @Test
    public void resolve_sourcesArtifact_doesNotBringTransitiveDependencies() {
        JkDependencySet deps = JkDependencySet.of()
                .and(JkCoordinateDependency.of(
                        "org.springframework.boot:spring-boot-starter-web:sources:1.5.10.RELEASE"))
               ;
        JkDependencyResolver resolver = JkDependencyResolver.of()
                .addRepos(JkRepo.ofMavenCentral());
        JkResolveResult resolveResult = resolver.resolve(deps);
        JkResolvedDependencyNode tree = resolveResult.getDependencyTree();
        System.out.println(resolveResult.getFiles());
        System.out.println(tree.toStringTree());


    }

    @Test
    public void resolve_usingDynamicVersion_ok() {
        JkModuleId jkModuleId = JkModuleId.of("org.springframework.boot:spring-boot-starter-web");
        JkDependencySet deps = JkDependencySet.of().and(jkModuleId.toCoordinate("1.4.+"));
        JkDependencyResolver resolver = JkDependencyResolver.of().addRepos(JkRepo.ofMavenCentral());
        JkResolvedDependencyNode tree = resolver.resolve(deps).assertNoError().getDependencyTree();
        System.out.println(tree.toStrings());
        JkResolvedDependencyNode.JkModuleNodeInfo moduleNodeInfo = tree.getFirst(jkModuleId).getModuleInfo();
        assertTrue(moduleNodeInfo.getDeclaredVersion().getValue().equals("1.4.+"));
        String resolvedVersionName = moduleNodeInfo.getResolvedVersion().getValue();
        assertEquals("1.4.7.RELEASE", resolvedVersionName);
    }

    @Test
    public void resolve_compileTransitivity_dontFetchRuntimeTransitiveDependencies() {
        JkDependencySet deps = JkDependencySet.of()
                .and("org.springframework.boot:spring-boot-starter:1.5.3.RELEASE", JkTransitivity.COMPILE);
        JkDependencyResolver resolver = JkDependencyResolver.of().addRepos(JkRepo.ofMavenCentral());
        JkResolveResult resolveResult = resolver.resolve(deps);
        boolean snakeyamlHere = resolveResult.contains( JkModuleId.of("org.yaml:snakeyaml"));
        assertFalse(snakeyamlHere);
    }

    @Test
    public void resolve_runtimeTransitivity_fetchRuntimeTransitiveDependencies() {
        JkDependencySet deps = JkDependencySet.of()
                .and("org.springframework.boot:spring-boot-starter:1.5.3.RELEASE", JkTransitivity.RUNTIME);
        JkDependencyResolver resolver = JkDependencyResolver.of().addRepos(JkRepo.ofMavenCentral());
        JkResolveResult resolveResult = resolver.resolve(deps);
        boolean snakeyamlHere = resolveResult.contains( JkModuleId.of("org.yaml:snakeyaml"));
        assertTrue(snakeyamlHere);
    }

    @Test
    public void resolve_fileDependenciesOnly_ok() throws URISyntaxException {
        Path dep0File = Paths.get(DependencySetResolutionIT.class.getResource("dep0").toURI());
        Path dep1File = Paths.get(DependencySetResolutionIT.class.getResource("dep1").toURI());
        JkDependencySet deps = JkDependencySet.of()
                .andFiles(dep0File)
                .andFiles(dep1File);
        JkDependencyResolver resolver = JkDependencyResolver.of();
        JkResolvedDependencyNode tree = resolver.resolve(deps).getDependencyTree();
        assertEquals(2, tree.toFlattenList().size());
        resolver = JkDependencyResolver.of().addRepos(JkRepo.ofMavenCentral());
        assertEquals(2, resolver.resolve(deps).getDependencyTree().toFlattenList().size());

    }

    @Test
    public void resolve_onlyFilesDependencies_ok() throws Exception {
        URL sampleJarUrl = DependencySetResolutionIT.class.getResource("myArtifactSample.jar");
        Path jarFile = Paths.get(sampleJarUrl.toURI());
        JkDependencySet dependencies = JkDependencySet.of()
                .andFiles(jarFile);
        JkDependencyResolver dependencyResolver = JkDependencyResolver.of()
                .addRepos(JkRepo.ofMavenCentral());
        JkResolveResult resolveResult = dependencyResolver.resolve(dependencies);
        Assert.assertEquals(1, resolveResult.getDependencyTree().getChildren().size());
    }

    @Test
    public void resolve_fileAndModuleDependencies_ok() throws Exception {
        URL sampleJarUrl = DependencySetResolutionIT.class.getResource("myArtifactSample.jar");
        Path jarFile = Paths.get(sampleJarUrl.toURI());
        JkDependencySet dependencies = JkDependencySet.of()
                .and(JkPopularLibs.GUAVA.toCoordinate("23.0"))
                .andFiles(jarFile);
        JkDependencyResolver dependencyResolver = JkDependencyResolver.of().addRepos(JkRepo.ofMavenCentral());
        JkResolveResult resolveResult = dependencyResolver.resolve(dependencies);
        Assert.assertEquals(2, resolveResult.getDependencyTree().getChildren().size());
        resolveResult.assertNoError();
    }

    @Test
    public void resolve_only1ModuleDependencies_ok() throws Exception {
        JkLog.Verbosity verbosity = JkLog.verbosity();
        JkDependencySet dependencies = JkDependencySet.of()
                .and(JkPopularLibs.GUAVA + ":23.0");
        JkDependencyResolver dependencyResolver = JkDependencyResolver.of().addRepos(JkRepo.ofMavenCentral());
        JkResolveResult resolveResult = dependencyResolver.resolve(dependencies);
        resolveResult.assertNoError();
        Assert.assertEquals(1, resolveResult.getDependencyTree().getChildren().size());
        JkLog.setVerbosity(verbosity);
    }

    @Test
    public void resolve_usingSeveralClassifierOnSingleLine_ok() {
        JkProject project = JkProject.of()
                .setJvmTargetVersion(JkJavaVersion.V11)
                .flatFacade()
                    .customizeCompileDeps(deps -> deps
                        .and("org.openjfx:javafx-controls:win:11.0.2", JkTransitivity.NONE)
                        .and("org.openjfx:javafx-controls:linux:11.0.2", JkTransitivity.NONE)
                        .and("org.openjfx:javafx-controls:mac:11.0.2", JkTransitivity.NONE))
                .getProject();
        project.setIncludeTextAndLocalDependencies(false);
        JkResolveResult resolveResult = project.compilation.resolveDependencies();
        resolveResult.getDependencyTree().toStrings().forEach(System.out::println);
        JkPathSequence paths = resolveResult.getFiles();
        paths.getEntries().forEach(path -> System.out.println(path.getFileName()));
        assertEquals(3, paths.getEntries().size());
    }

    @Test
    public void resolve_usingSeveralClassifiersIncludingDefaultOne_ok() {
        JkProject project = JkProject.of()
                .setJvmTargetVersion(JkJavaVersion.V11)
                .flatFacade()
                .customizeCompileDeps(deps -> deps
                        .and("org.openjfx:javafx-controls:win:11.0.2", JkTransitivity.NONE))
                .getProject();
        project.setIncludeTextAndLocalDependencies(false);
        JkResolveResult resolveResult = project.compilation.resolveDependencies();
        resolveResult.getDependencyTree().toStrings().forEach(System.out::println);
        JkPathSequence paths = resolveResult.getFiles();
        paths.getEntries().forEach(path -> System.out.println(path.getFileName()));
        assertEquals(1, paths.getEntries().size());
        // the order Ivy resolve classifiers cannot be controlled
    }


}

package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.depmanagement.JkCoordinate.ConflictStrategy;
import dev.jeka.core.api.depmanagement.publication.JkIvyPublication;
import dev.jeka.core.api.depmanagement.publication.JkMavenPublication;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.project.JkProjectPublications;
import dev.jeka.core.api.utils.JkUtilsPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Only the runtime classpath is published, with the 'compile' scope for dependencies also needed for compiling.
 */
class PublishedDependenciesTest {

    private static final String GSON = "com.google.code.gson:gson";

    private static final String LOMBOK = "org.projectlombok:lombok";

    private static final String POSTGRESQL = "org.postgresql:postgresql";

    private static final Map<String, JkTransitivity> EXPECTED =
            Map.of(GSON, JkTransitivity.COMPILE, POSTGRESQL, JkTransitivity.RUNTIME);

    @Test
    void maven_depsFile_runtimeClasspathIsPublished(@TempDir Path dir) {
        assertEquals(EXPECTED, mavenScopes(projectWithDepsFile(dir)));
    }

    @Test
    void maven_javaDeclared_runtimeClasspathIsPublished(@TempDir Path dir) {
        assertEquals(EXPECTED, mavenScopes(projectWithJavaDeps(dir)));
    }

    @Test
    void maven_publishedPomHasConventionalScopes(@TempDir Path dir) {
        Path repo = dir.resolve("repo");
        JkProject project = projectWithDepsFile(dir);
        JkMavenPublication.of(project.asBuildable())
                .removeAllArtifacts()
                .setRepos(JkRepo.of(repo).toSet())
                .publish();

        String pom = new String(JkUtilsPath.readAllBytes(repo.resolve("my/lib/1.0/lib-1.0.pom")))
                .replaceAll("\\s", "");
        assertTrue(pom.contains("<artifactId>gson</artifactId><version>2.11.0</version><scope>compile</scope>"), pom);
        assertTrue(pom.contains("<artifactId>postgresql</artifactId><version>42.7.4</version><scope>runtime</scope>"),
                pom);
        assertFalse(pom.contains("lombok"), pom);
    }

    @Test
    void maven_customizeDependencies_isEffective(@TempDir Path dir) {
        JkMavenPublication publication = JkMavenPublication.of(projectWithDepsFile(dir).asBuildable())
                .customizeDependencies(deps -> deps
                        .withTransitivity(GSON, JkTransitivity.RUNTIME)
                        .withTransitivity(POSTGRESQL, JkTransitivity.COMPILE)
                        .and(LOMBOK + ":1.18.38", JkTransitivity.COMPILE));
        assertEquals(Map.of(GSON, JkTransitivity.RUNTIME,
                POSTGRESQL, JkTransitivity.COMPILE,
                LOMBOK, JkTransitivity.COMPILE), mavenScopes(publication));
    }

    @Test
    void maven_declaredForCompileAndRuntime_publishedOnceWithCompileScope() {
        JkDependencySet compile = JkDependencySet.of().and(GSON + ":2.11.0");
        JkDependencySet runtime = JkDependencySet.of().and(GSON + ":2.11.0").and(compile);
        JkDependencySet published = JkMavenPublication.computeMavenPublishDependencies(compile, runtime,
                ConflictStrategy.FAIL);
        assertEquals(1, published.getEntries().size());
        assertEquals(JkTransitivity.COMPILE, published.get(GSON).getTransitivity());
    }

    @Test
    void maven_distinctClassifiers_areAllPublished() {
        JkDependencySet deps = JkDependencySet.of().and("org.lwjgl:lwjgl:3.3.6")
                .and("org.lwjgl:lwjgl:natives-linux:3.3.6");
        JkDependencySet published = JkMavenPublication.computeMavenPublishDependencies(deps, deps,
                ConflictStrategy.FAIL);
        assertEquals(2, published.getEntries().size());
    }

    @Test
    void maven_exclusionsArePublished() {
        JkDependencySet deps = JkDependencySet.of().and(GSON + ":2.11.0").withLocalExclusions("a:b");
        JkDependencySet published = JkMavenPublication.computeMavenPublishDependencies(deps, deps,
                ConflictStrategy.FAIL);
        assertEquals(1, published.get(GSON).getExclusions().size());
    }

    @Test
    void ivy_runtimeClasspathIsPublished(@TempDir Path dir) {
        Map<String, String> configurations = ivyConfigurations(
                JkProjectPublications.ivyPublication(projectWithDepsFile(dir)).getDependencies());
        assertEquals(Map.of(
                GSON, "compile->compile(*),master(*);runtime -> runtime(*),master(*)",
                POSTGRESQL, "runtime->runtime(*),master(*)"), configurations);
    }

    @Test
    void ivy_setDependencies_runtimeClasspathIsPublished() {
        JkVersionProvider versions = JkVersionProvider.of(POSTGRESQL, "42.7.4");
        JkDependencySet compile = JkDependencySet.of().and(GSON + ":2.11.0").and(LOMBOK + ":1.18.38");
        JkDependencySet runtime = JkDependencySet.of().and(GSON + ":2.11.0").and(POSTGRESQL)
                .withVersionProvider(versions);
        JkDependencySet test = JkDependencySet.of().and("org.junit.jupiter:junit-jupiter:5.12.1");

        Map<String, String> configurations = ivyConfigurations(JkIvyPublication.of()
                .setDependencies(compile, runtime, test).getDependencies());

        assertFalse(configurations.containsKey(LOMBOK));
        assertTrue(configurations.get(GSON).startsWith("compile,runtime ->"));
        assertTrue(configurations.get(POSTGRESQL).startsWith("runtime ->"), configurations.get(POSTGRESQL));
        assertTrue(configurations.get("org.junit.jupiter:junit-jupiter").startsWith("test ->"));
    }

    private static Map<String, JkTransitivity> mavenScopes(JkProject project) {
        return mavenScopes(JkMavenPublication.of(project.asBuildable()));
    }

    private static Map<String, JkTransitivity> mavenScopes(JkMavenPublication publication) {
        Map<String, JkTransitivity> result = new LinkedHashMap<>();
        publication.getDependencies().getCoordinateDependencies().forEach(dep ->
                result.put(dep.getCoordinate().getModuleId().toString(), dep.getTransitivity()));
        return result;
    }

    private static Map<String, String> ivyConfigurations(JkQualifiedDependencySet dependencies) {
        Map<String, String> result = new LinkedHashMap<>();
        dependencies.getEntries().forEach(dep -> result.put(
                dep.getCoordinateDependency().getCoordinate().getModuleId().toString(), dep.getQualifier()));
        return result;
    }

    private static JkProject projectWithDepsFile(Path dir) {
        JkUtilsPath.write(dir.resolve(JkProject.PROJECT_DEPENDENCIES_FILE), String.join("\n",
                "[version]",
                POSTGRESQL + ":42.7.4",
                "[compile]",
                GSON + ":2.11.0",
                "[compile-only]",
                LOMBOK + ":1.18.38",
                "[runtime]",
                POSTGRESQL,
                "[test]",
                "org.junit.jupiter:junit-jupiter:5.12.1",
                "[processor]",
                LOMBOK + ":1.18.38").getBytes());
        return JkProject.of().setBaseDir(dir).setModuleId("my:lib").setVersion("1.0");
    }

    private static JkProject projectWithJavaDeps(Path dir) {
        JkProject project = JkProject.of().setBaseDir(dir).setModuleId("my:lib").setVersion("1.0");
        project.flatFacade.addCompileOnlyDeps(LOMBOK + ":1.18.38");
        project.flatFacade.dependencies.compile.add(GSON + ":2.11.0");
        project.flatFacade.dependencies.runtime.add(POSTGRESQL + ":42.7.4");
        project.flatFacade.dependencies.test.add("org.junit.jupiter:junit-jupiter:5.12.1");
        return project;
    }

}

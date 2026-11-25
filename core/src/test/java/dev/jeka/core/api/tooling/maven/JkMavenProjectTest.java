/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.jeka.core.api.tooling.maven;

import dev.jeka.core.api.depmanagement.JkCoordinateDependency;
import dev.jeka.core.api.depmanagement.JkDependency;
import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.project.JkProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Maven is replaced by a 'mvnw' script providing the effective POM.
 */
@DisabledOnOs(OS.WINDOWS)
class JkMavenProjectTest {

    @TempDir
    Path rootDir;

    @Test
    void scopes() {
        Path dir = mavenProject("", """
                <dependency><groupId>g</groupId><artifactId>no-scope</artifactId><version>1</version></dependency>
                <dependency><groupId>g</groupId><artifactId>compile</artifactId><version>2</version><scope>compile</scope></dependency>
                <dependency><groupId>g</groupId><artifactId>provided</artifactId><version>3</version><scope>provided</scope></dependency>
                <dependency><groupId>g</groupId><artifactId>runtime</artifactId><version>4</version><scope>runtime</scope></dependency>
                <dependency><groupId>g</groupId><artifactId>test</artifactId><version>5</version><scope>test</scope></dependency>
                <dependency><groupId>g</groupId><artifactId>range</artifactId><version>[1, 2)</version><scope>test</scope></dependency>
                <dependency><groupId>g</groupId><artifactId>system</artifactId><version>6</version><scope>system</scope>
                  <systemPath>%s/vendor/local.jar</systemPath></dependency>
                """.formatted(rootDir.resolve("app")));

        assertEquals("""
                [compile]
                g:no-scope:1
                g:compile:2

                [compile-only]
                g:provided:3
                vendor/local.jar

                [runtime]
                g:runtime:4

                [test]
                g:test:5
                g:range:[1,2)
                """, txt(dir));
        assertEquals("""
                Compile:
                  .add("g:no-scope:1")
                  .add("g:compile:2")
                  .add("g:provided:3")
                  .add(getBaseDir().resolve("vendor/local.jar"))
                Runtime:
                  .add("g:runtime:4")
                  .remove("g:provided")
                  .remove(getBaseDir().resolve("vendor/local.jar"))
                Test:
                  .add("g:test:5")
                  .add("g:range:[1,2)")
                """, JkMavenProject.of(dir).getDependencyAsJeKaCode(2));

        // The text can be used as is
        JkPathFile.of(dir.resolve(JkProject.PROJECT_DEPENDENCIES_FILE))
                .write(JkMavenProject.of(dir).getDependenciesAsTxt());
        JkProject project = JkProject.of().setBaseDir(dir);
        assertEquals(List.of("g:no-scope:1", "g:compile:2", "g:provided:3"),
                coordinates(project.compilation.dependencies.get()));
        assertEquals(List.of("g:runtime:4", "g:no-scope:1", "g:compile:2"),
                coordinates(project.pack.runtimeDependencies.get()));
        assertTrue(coordinates(project.test.compilation.dependencies.get()).containsAll(
                List.of("g:test:5", "g:range:[1,2)", "g:provided:3", "g:runtime:4", "g:compile:2")));
    }

    @Test
    void classifiersTypesAndExclusions() {
        Path dir = mavenProject("", """
                <dependency><groupId>g</groupId><artifactId>jar</artifactId><version>1</version><type>jar</type>
                  <exclusions>
                    <exclusion><groupId>a</groupId><artifactId>b</artifactId></exclusion>
                    <exclusion><groupId>c</groupId><artifactId>*</artifactId></exclusion>
                  </exclusions></dependency>
                <dependency><groupId>g</groupId><artifactId>classifier</artifactId><version>1</version><classifier>linux</classifier></dependency>
                <dependency><groupId>g</groupId><artifactId>test-jar</artifactId><version>1</version><type>test-jar</type>
                  <exclusions><exclusion><groupId>a</groupId><artifactId>b</artifactId></exclusion></exclusions></dependency>
                <dependency><groupId>g</groupId><artifactId>zip</artifactId><version>1</version><type>zip</type><classifier>doc</classifier></dependency>
                <dependency><groupId>g</groupId><artifactId>alone</artifactId><version>1</version>
                  <exclusions><exclusion><groupId>*</groupId><artifactId>*</artifactId></exclusion></exclusions></dependency>
                <dependency><groupId>g</groupId><artifactId>pom</artifactId><version>1</version><type>pom</type></dependency>
                """);

        String comment = "  # transitive dependencies are not included, declare them if needed";
        assertEquals("""
                [compile]
                g:jar:1
                    !a:b
                    !c:*
                g:classifier:linux:1%1$s
                g:test-jar:tests:1%1$s
                    !a:b
                g:zip:doc:zip:1%1$s
                g:alone::jar:1
                # g:pom:1 has type 'pom': declare the dependencies it brings instead

                [compile-only]

                [runtime]

                [test]
                """.formatted(comment), txt(dir));
        assertEquals("""
                Compile:
                    .addWithExclusions("g:jar:1", "a:b", "c:*")
                    .add("g:classifier:linux:1", JkTransitivity.RUNTIME)
                    .add(JkCoordinateDependency.of("g:test-jar:tests:1").withTransitivity(JkTransitivity.RUNTIME).andExclusions("a:b"))
                    .add("g:zip:doc:zip:1", JkTransitivity.RUNTIME)
                    .add("g:alone::jar:1")
                    // g:pom:1 has type 'pom': declare the dependencies it brings instead
                Runtime:
                Test:
                """, JkMavenProject.of(dir).getDependencyAsJeKaCode(4));

        JkPathFile.of(dir.resolve(JkProject.PROJECT_DEPENDENCIES_FILE))
                .write(JkMavenProject.of(dir).getDependenciesAsTxt());
        JkDependencySet compileDeps = JkProject.of().setBaseDir(dir).compilation.dependencies.get();
        assertEquals(List.of("g:jar:1", "g:classifier:linux::1", "g:test-jar:tests::1", "g:zip:doc:zip:1", "g:alone::jar:1"),
                coordinates(compileDeps));
        assertEquals(2, ((JkCoordinateDependency) compileDeps.getEntries().get(0)).getExclusions().size());
    }

    @Test
    void dependencyManagement_ofPomAndParents() {
        JkPathFile.of(rootDir.resolve("pom.xml")).write(pom("""
                <parent><groupId>org.ext</groupId><artifactId>ext-parent</artifactId><version>3</version><relativePath/></parent>
                <groupId>g</groupId><artifactId>parent</artifactId><version>1</version>
                <dependencyManagement><dependencies>
                  <dependency><groupId>${project.groupId}</groupId><artifactId>sibling</artifactId><version>${project.version}</version></dependency>
                  <dependency><groupId>a</groupId><artifactId>overridden</artifactId><version>1</version></dependency>
                </dependencies></dependencyManagement>
                """));
        String parent = "<parent><groupId>g</groupId><artifactId>parent</artifactId><version>1</version></parent>";
        Path dir = mavenProject(parent + """
                <artifactId>app</artifactId>
                <dependencyManagement><dependencies>
                  <dependency><groupId>a</groupId><artifactId>bom</artifactId><version>${bom.version}</version><type>pom</type><scope>import</scope></dependency>
                  <dependency><groupId>a</groupId><artifactId>overridden</artifactId><version>2</version></dependency>
                </dependencies></dependencyManagement>
                """, """
                <dependency><groupId>a</groupId><artifactId>b</artifactId><version>7</version></dependency>
                """);

        assertEquals("""
                [version]
                a:bom:7@pom
                org.ext:ext-parent:3@pom
                a:overridden:2
                g:sibling:1

                [compile]
                a:b:7

                [compile-only]

                [runtime]

                [test]
                """, txt(dir));
        assertEquals("""
                Compile:
                  .addVersionProvider(JkVersionProvider.of()
                    .andBom("a:bom:7")
                    .andBom("org.ext:ext-parent:3")
                    .and("a:overridden", "2")
                    .and("g:sibling", "1"))
                  .add("a:b:7")
                Runtime:
                Test:
                """, JkMavenProject.of(dir).getDependencyAsJeKaCode(2));
    }

    @Test
    void externalParentManagingNoVersion_noBom() {
        Path dir = mavenProject("""
                <parent><groupId>org.ext</groupId><artifactId>ext-parent</artifactId><version>3</version></parent>
                <artifactId>app</artifactId>
                """, "", "");
        assertTrue(txt(dir).startsWith("[compile]"));
    }

    private Path mavenProject(String rawPomContent, String dependencies) {
        return mavenProject(rawPomContent, dependencies, """
                <dependency><groupId>a</groupId><artifactId>from-bom</artifactId><version>1</version></dependency>
                """);
    }

    private Path mavenProject(String rawPomContent, String dependencies, String effectiveManagedDeps) {
        Path dir = rootDir.resolve("app");
        JkPathFile.of(dir.resolve("pom.xml")).write(pom(rawPomContent));
        JkPathFile.of(dir.resolve("effective.xml")).write(pom("""
                <groupId>g</groupId><artifactId>app</artifactId><version>1</version>
                <properties><bom.version>7</bom.version></properties>
                <dependencyManagement><dependencies>%s</dependencies></dependencyManagement>
                <dependencies>%s</dependencies>
                """.formatted(effectiveManagedDeps, dependencies)));
        JkPathFile.of(dir.resolve("mvnw")).write("""
                #!/bin/sh
                for arg in "$@"; do
                  case "$arg" in
                    -Doutput=*) cp effective.xml "${arg#-Doutput=}" ;;
                  esac
                done
                """).setPosixExecPermissions();
        return dir;
    }

    private static String pom(String content) {
        return "<project><modelVersion>4.0.0</modelVersion>" + content + "</project>";
    }

    // Ignore the extra blank lines
    private static String txt(Path dir) {
        return JkMavenProject.of(dir).getDependenciesAsTxt().strip().replaceAll("\n{3,}", "\n\n") + "\n";
    }

    private static List<String> coordinates(JkDependencySet dependencySet) {
        Stream<JkDependency> dependencies = dependencySet.getEntries().stream();
        return dependencies
                .filter(JkCoordinateDependency.class::isInstance)
                .map(dependency -> ((JkCoordinateDependency) dependency).getCoordinate().toString())
                .toList();
    }

}

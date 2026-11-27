/*
 * Copyright 2014-2025  the original author or authors.
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

import dev.jeka.core.api.depmanagement.JkCoordinate;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.JkVersionProvider;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.marshalling.xml.JkDomDocument;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class JkPomTest {

    @Test
    void withResolvedProperties() {
        JkRepoSet repos = JkRepoSet.of();
        JkPom pom = JkPom.of(JkPomTest.class.getResourceAsStream("pom-sample.xml"));
        JkPom effectivePom = pom.toEffectivePom(repos);
        System.out.println( JkDomDocument.of(pom.toDocument()).toXml() );
        assertEquals("6.1.3", effectivePom.toVersionProvider(repos)
                .getVersionOf("org.springframework:spring-core"));
    }


    @Test
    void ofEffectivePom_versionManagedByGrandParent_found() throws IOException {
        Path repoDir = Files.createTempDirectory("jk-pom-repo");
        String group = "org.example.grandparent" + System.nanoTime();  // avoid cached POMs of previous runs
        writePom(repoDir, group, "p3", null, "com.google.guava", "guava", "33.0.0-jre");
        writePom(repoDir, group, "p2", "p3", "org.slf4j", "slf4j-api", "2.0.17");
        writePom(repoDir, group, "p1", "p2", null, null, null);
        JkRepoSet repos = JkRepoSet.of(JkRepo.of(repoDir));

        JkPom pom = JkPom.ofEffectivePom(JkCoordinate.of(group + ":p1:1.0@pom"), repos);
        JkVersionProvider versionProvider = pom.toVersionProvider(repos);

        assertEquals("2.0.17", versionProvider.getVersionOf("org.slf4j:slf4j-api"));
        assertEquals("33.0.0-jre", versionProvider.getVersionOf("com.google.guava:guava"));
        JkPathTree.of(repoDir).deleteRoot();
    }

    // A dependency of type 'pom' without scope 'import' is a managed version, not an imported BOM (as in Maven).
    @Test
    void toVersionProvider_pomTypeWithoutImportScope_notImported() throws IOException {
        Path repoDir = Files.createTempDirectory("jk-pom-repo");
        String group = "org.example.pomtype" + System.nanoTime();
        writePom(repoDir, group, "bundle", null, "org.apache.httpcomponents.client5", "httpclient5", "5.3.1");
        writeRawPom(repoDir, group, "bom", "<dependency><groupId>" + group + "</groupId><artifactId>bundle</artifactId>"
                + "<version>1.0</version><type>pom</type></dependency>");
        JkRepoSet repos = JkRepoSet.of(JkRepo.of(repoDir));

        JkVersionProvider versionProvider = JkPom.ofEffectivePom(JkCoordinate.of(group + ":bom:1.0@pom"), repos)
                .toVersionProvider(repos);

        assertNull(versionProvider.getVersionOf("org.apache.httpcomponents.client5:httpclient5"));
        assertEquals("1.0", versionProvider.getVersionOf(group + ":bundle"));
        JkPathTree.of(repoDir).deleteRoot();
    }

    // As in Maven, versions declared in the BOM win over imported ones, and the first import wins over the next ones.
    @Test
    void toVersionProvider_sameModuleInImportedBoms_ownThenFirstImportWins() throws IOException {
        Path repoDir = Files.createTempDirectory("jk-pom-repo");
        String group = "org.example.precedence" + System.nanoTime();
        writeRawPom(repoDir, group, "imported1", managed("org.slf4j", "slf4j-api", "2.0.1")
                + managed("com.google.guava", "guava", "31.0-jre"));
        writeRawPom(repoDir, group, "imported2", managed("org.slf4j", "slf4j-api", "2.0.2")
                + managed("com.google.guava", "guava", "32.0-jre"));
        String imports = "<dependency><groupId>" + group + "</groupId><artifactId>imported1</artifactId>"
                + "<version>1.0</version><type>pom</type><scope>import</scope></dependency>"
                + "<dependency><groupId>" + group + "</groupId><artifactId>imported2</artifactId>"
                + "<version>1.0</version><type>pom</type><scope>import</scope></dependency>";
        writeRawPom(repoDir, group, "bom", imports + managed("org.slf4j", "slf4j-api", "2.0.17"));
        JkRepoSet repos = JkRepoSet.of(JkRepo.of(repoDir));

        JkVersionProvider versionProvider = JkPom.ofEffectivePom(JkCoordinate.of(group + ":bom:1.0@pom"), repos)
                .toVersionProvider(repos);

        assertEquals("2.0.17", versionProvider.getVersionOf("org.slf4j:slf4j-api"));
        assertEquals("31.0-jre", versionProvider.getVersionOf("com.google.guava:guava"));
        JkPathTree.of(repoDir).deleteRoot();
    }

    private static String managed(String group, String artifactId, String version) {
        return String.format("<dependency><groupId>%s</groupId><artifactId>%s</artifactId><version>%s</version>"
                + "</dependency>", group, artifactId, version);
    }

    private static void writeRawPom(Path repoDir, String group, String artifactId, String managedDependencies)
            throws IOException {
        String content = String.format("<project><modelVersion>4.0.0</modelVersion><groupId>%s</groupId>"
                + "<artifactId>%s</artifactId><version>1.0</version><packaging>pom</packaging>"
                + "<dependencyManagement><dependencies>%s</dependencies></dependencyManagement></project>",
                group, artifactId, managedDependencies);
        Path dir = repoDir.resolve(group.replace('.', '/')).resolve(artifactId).resolve("1.0");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(artifactId + "-1.0.pom"), content);
    }

    private static void writePom(Path repoDir, String group, String artifactId, String parentArtifactId,
                                 String managedGroup, String managedArtifact, String managedVersion) throws IOException {
        String parent = parentArtifactId == null ? "" : String.format(
                "<parent><groupId>%s</groupId><artifactId>%s</artifactId><version>1.0</version></parent>",
                group, parentArtifactId);
        String depManagement = managedGroup == null ? "" : String.format(
                "<dependencyManagement><dependencies><dependency><groupId>%s</groupId><artifactId>%s</artifactId>"
                + "<version>%s</version></dependency></dependencies></dependencyManagement>",
                managedGroup, managedArtifact, managedVersion);
        String content = String.format("<project><modelVersion>4.0.0</modelVersion>%s<groupId>%s</groupId>"
                + "<artifactId>%s</artifactId><version>1.0</version><packaging>pom</packaging>%s</project>",
                parent, group, artifactId, depManagement);
        Path dir = repoDir.resolve(group.replace('.', '/')).resolve(artifactId).resolve("1.0");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(artifactId + "-1.0.pom"), content);
    }

}
package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.depmanagement.artifact.JkArtifactLocator;
import dev.jeka.core.api.depmanagement.publication.JkMavenPublication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JkMavenPublicationInfoTest {

    @Test
    void info_bomIsResolvedFromDownloadRepos(@TempDir Path dir) throws Exception {
        String group = "jeka.test.b" + UUID.randomUUID().toString().replace("-", "");  // not in cache
        Path bomDir = Files.createDirectories(
                dir.resolve("download").resolve(group.replace('.', '/')).resolve("bom/1.0"));
        Files.write(bomDir.resolve("bom-1.0.pom"), ("<project><modelVersion>4.0.0</modelVersion>"
                + "<groupId>" + group + "</groupId><artifactId>bom</artifactId><version>1.0</version>"
                + "<packaging>pom</packaging><dependencyManagement><dependencies><dependency>"
                + "<groupId>" + group + "</groupId><artifactId>lib</artifactId><version>2.3</version>"
                + "</dependency></dependencies></dependencyManagement></project>").getBytes());
        Path publishDir = Files.createDirectories(dir.resolve("publish"));
        JkRepoSet downloadRepos = JkRepo.of(dir.resolve("download")).toSet();

        String info = JkMavenPublication.of(JkArtifactLocator.of(dir, "lib"))
                .setModuleId("a:b")
                .setVersion("1")
                .setRepos(JkRepo.of(publishDir).toSet())
                .setBomResolutionRepos(() -> downloadRepos)
                .customizeDependencies(deps -> JkDependencySet.of().and(group + ":lib")
                        .withVersionProvider(JkVersionProvider.of().andBom(group + ":bom:1.0")))
                .info();

        assertTrue(info.contains(group + ":lib:2.3"), info);
    }

}

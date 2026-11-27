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

package dev.jeka.core.api.depmanagement.resolution;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.file.JkPathTree;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JkDependencyResolverConflictTest {

    // As kotlin-compiler:2.1.21: 'org.jetbrains:annotations' 13.0 is met first, deeper in the tree, then
    // evicted by 23.0.0. The jar of the winning version must be in the resolved files.
    @Test
    void resolve_evictedVersionMetFirst_winnerFilePresent() throws IOException {
        Path repoDir = Files.createTempDirectory("jk-conflict-repo");
        String group = "org.example.conflict" + System.nanoTime();  // avoid cached modules of previous runs
        writeModule(repoDir, group, "annotations", "13.0");
        writeModule(repoDir, group, "annotations", "23.0.0");
        writeModule(repoDir, group, "stdlib", "1.0", group + ":annotations:13.0");
        writeModule(repoDir, group, "stdlib-jdk8", "1.0", group + ":stdlib:1.0");
        writeModule(repoDir, group, "coroutines", "1.0", group + ":annotations:23.0.0");
        writeModule(repoDir, group, "compiler", "1.0", group + ":stdlib-jdk8:1.0", group + ":coroutines:1.0");

        JkResolveResult result = JkDependencyResolver.of(JkRepo.of(repoDir).toSet())
                .resolve(JkDependencySet.of().and(group + ":compiler:1.0"));

        List<String> fileNames = result.getFiles().getEntries().stream()
                .map(path -> path.getFileName().toString())
                .collect(Collectors.toList());
        assertEquals(List.of("annotations-23.0.0.jar"), fileNames.stream()
                .filter(name -> name.startsWith("annotations")).collect(Collectors.toList()), fileNames.toString());
        JkPathTree.of(repoDir).deleteRoot();
    }

    private static void writeModule(Path repoDir, String group, String artifactId, String version,
                                    String... dependencies) throws IOException {
        StringBuilder deps = new StringBuilder();
        for (String dependency : dependencies) {
            String[] items = dependency.split(":");
            deps.append(String.format("<dependency><groupId>%s</groupId><artifactId>%s</artifactId>"
                    + "<version>%s</version></dependency>", items[0], items[1], items[2]));
        }
        String pom = String.format("<project><modelVersion>4.0.0</modelVersion><groupId>%s</groupId>"
                + "<artifactId>%s</artifactId><version>%s</version><dependencies>%s</dependencies></project>",
                group, artifactId, version, deps);
        Path dir = repoDir.resolve(group.replace('.', '/')).resolve(artifactId).resolve(version);
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(artifactId + "-" + version + ".pom"), pom);
        Files.writeString(dir.resolve(artifactId + "-" + version + ".jar"), "fake jar");
    }

}

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

package dev.jeka.core.api.j2e;

import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.project.JkProject;

import java.nio.file.Path;
import java.util.function.Consumer;

public class JkJ2eWarProjectAdapter {

    private Path extraStaticResourcePath;

    private String webappPath;

    private boolean generateExploded;

    private JkJ2eWarProjectAdapter() {
    }

    public static JkJ2eWarProjectAdapter of() {
        return new JkJ2eWarProjectAdapter();
    }

    public JkJ2eWarProjectAdapter setExtraStaticResourcePath(Path extraStaticResourcePath) {
        this.extraStaticResourcePath = extraStaticResourcePath;
        return this;
    }

    public JkJ2eWarProjectAdapter setWebappPath(String webappPath) {
        this.webappPath = webappPath;
        return this;
    }


    public JkJ2eWarProjectAdapter setGenerateExploded(boolean generateExploded) {
        this.generateExploded = generateExploded;
        return this;
    }

    /**
     * Configures the specified project to produce and publish WAR archive.
     */
    public void configure(JkProject project) {
        JkArtifactId warArtifact = JkArtifactId.ofMainArtifact("war");
        Path warFile = project.artifactLocator.getArtifactPath(warArtifact);
        Consumer<Path> warMaker = path -> generateWar(project, path);
        project.packActions.set(() -> warMaker.accept(warFile));
    }

    public void generateWar(JkProject project, Path targetPath) {
        final Path effectiveWebappPath;
        if (webappPath != null) {
            effectiveWebappPath = project.getBaseDir().resolve(webappPath);
        } else {
            Path src =  project.compilation.layout.getSources().toList().get(0).getRoot();
            effectiveWebappPath = src.resolveSibling("webapp");
        }
        generateWar(project, targetPath, effectiveWebappPath, extraStaticResourcePath, generateExploded);
    }

    private static void generateWar(JkProject project, Path targetFile, Path webappPath, Path extraStaticResourcePath,
                                    boolean generateDir) {

        JkJ2eWarArchiver archiver = JkJ2eWarArchiver.of()
                .setClassDir(project.compilation.layout.resolveClassDir())
                .setExtraStaticResourceDir(extraStaticResourcePath)
                .setLibs(project.packaging.resolveRuntimeDependenciesAsFiles())
                .setWebappDir(webappPath);
        project.compilation.runIfNeeded();
        project.testing.runIfNeeded();
        if (generateDir) {
            Path dirPath = project.getOutputDir().resolve("j2e-war");
            archiver.generateWarDir(dirPath);
            JkPathTree.of(dirPath).zipTo(targetFile);
        } else {
            archiver.generateWarFile(targetFile);
        }
    }



}

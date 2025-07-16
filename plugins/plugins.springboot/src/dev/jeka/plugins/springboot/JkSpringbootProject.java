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

package dev.jeka.plugins.springboot;

import dev.jeka.core.api.depmanagement.JkVersionProvider;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.j2e.JkJ2eWarProjectAdapter;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.project.JkProjectPackaging;
import dev.jeka.core.api.system.JkLog;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * Class for adding Spring-Boot configuration to  {@link JkProject}s.
 */
public final class JkSpringbootProject {

    public enum SpringbootScaffoldTemplate {
        LIB,
        PROPS
    }

    public static final JkArtifactId ORIGINAL_ARTIFACT = JkArtifactId.of("original", "jar");

    // In some test scenario, we might want to inject a specific version of JeKA springboot plugin.
    // In this case we generally refers to a jar file on file system
    public static final String OVERRIDE_SCAFFOLDED_SPRINGBOOT_PLUGIN_DEPENDENCY_PROP_NAME
            = "jeka.springboot.plugin.dependency";

    static final String BOM_COORDINATE = "org.springframework.boot:spring-boot-dependencies:";

    private final JkProject project;


    private JkSpringbootProject(JkProject project) {
        this.project = project;
    }

    /**
     * Creates a {@link JkSpringbootProject} from the specified {@link JkProject}.
     */
    public static JkSpringbootProject of(JkProject project) {
        return new JkSpringbootProject(project);
    }

    /**
     * Configures the underlying project for Spring-Boot.
     * @param createBootJar       Should create or not, a bootable jar file. The bootable jar file will be set as the default artifact.
     * @param createWarFile       Should create or not, a .war file to be deployed in application server
     * @param createOriginalJar   Should create or not, the original jar, meaning the jar containing the application classes, without dependencies.
     */
    public JkSpringbootProject configure(boolean createBootJar, boolean createWarFile,
                          boolean createOriginalJar) {

        // run tests in forked mode
        project.test.processor.setForkingProcess(true);

        project.pack.actions.replaceOrAppend(JkProjectPackaging.CREATE_JAR_ACTION, () -> {});

        // define bootable jar as main artifact
        if (createBootJar) {
            JkArtifactId artifactId = JkArtifactId.MAIN_JAR_ARTIFACT_ID;
            Path artifactFile = project.artifactLocator.getArtifactPath(artifactId);
            project.pack.setJarMaker(this::createBootJar);
            project.pack.actions.replaceOrAppend(JkProjectPackaging.CREATE_JAR_ACTION, () -> createBootJar(artifactFile));
        }
        if (createWarFile) {
            JkArtifactId artifactId = JkArtifactId.ofMainArtifact("war");
            Path artifactFile = project.artifactLocator.getArtifactPath(artifactId);
            Consumer<Path> warMaker = path -> JkJ2eWarProjectAdapter.of().generateWar(project, path);
            project.pack.actions.replaceOrAppend("make-war-file", () -> warMaker.accept(artifactFile) );
        }
        if (createOriginalJar) {
            Path artifactFile = project.artifactLocator.getArtifactPath(ORIGINAL_ARTIFACT);
            Consumer<Path> makeBinJar = project.pack::createBinJar;
            project.pack.actions.replaceOrAppend("make-original-jar", () -> makeBinJar.accept(artifactFile));
        }

        // To deploy spring-Boot app in a container, we don't need to create a jar
        // This is more efficient to keep the structure exploded to have efficient image layering.
        // In this case, just copy manifest in class dir is enough.
        if (!createBootJar && !createOriginalJar && !createWarFile) {
            project.pack.actions.replaceOrAppend("include-manifest", project.pack::copyManifestInClassDir);
        }

        project.pack.setDetectMainClass(true);
        project.pack.setMainClassFinder(() -> {
            try {
                return JkSpringbootJars.findMainClassName(project.compilation.layout.resolveClassDir());
            } catch (IllegalStateException e) {
                JkLog.info("No Springboot application class found in class dir. Force recompile and re-search.");
                project.compilation.runIfNeeded();
                return JkSpringbootJars.findMainClassName(project.compilation.layout.resolveClassDir());
            }

        });

        return this;
    }

    /**
     * Configures the underlying project for Spring-Boot using sensitive default
     * @see #configure(boolean, boolean, boolean)
     */
    public JkSpringbootProject configure() {
        return configure(true, false, false);
    }

    /**
     * Includes org.springframework.boot:spring-boot-dependencies BOM in project dependencies.
     * This is the version that determines the effective Spring-Boot version to use.
     * @param version The Spring-Boot version to use.
     * @return This oject for chaining.
     */
    public JkSpringbootProject includeParentBom(String version) {
        project.compilation.dependencies
                .addVersionProvider(JkVersionProvider.of().andBom(BOM_COORDINATE + version));
        return this;
    }

    /**
     * Adds the specified Spring repository to the download repo. It may be necessary
     * to use pre-release versions.
     */
    public JkSpringbootProject addSpringRepo(JkSpringRepo springRepo) {
        JkDependencyResolver dependencyResolver = project.dependencyResolver;
        dependencyResolver.addRepos(springRepo.get());
        return this;
    }

    /**
     * Creates the bootable jar at the conventional location.
     * @see #createBootJar(Path)
     */
    public Path createBootJar() {
        Path target = project.artifactLocator.getMainArtifactPath();
        createBootJar(target);
        return target;
    }

    /**
     * Creates the bootable jar at the specified location.
     */
    public void createBootJar(Path target) {
        JkPathTree classTree = JkPathTree.of(project.compilation.layout.resolveClassDir());
        if (!classTree.exists()) {
            project.compilation.runIfNeeded();
        }
        JkDependencyResolver dependencyResolver = project.dependencyResolver;
        final List<Path> embeddedJars = project.pack.resolveRuntimeDependenciesAsFiles();
        JkSpringbootJars.createBootJar(classTree, embeddedJars, dependencyResolver.getRepos(), target,
                project.pack.getManifest());
    }

}

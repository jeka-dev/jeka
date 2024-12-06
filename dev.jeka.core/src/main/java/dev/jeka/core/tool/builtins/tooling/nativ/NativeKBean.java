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

package dev.jeka.core.tool.builtins.tooling.nativ;

import dev.jeka.core.api.depmanagement.JkCoordinate;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import dev.jeka.core.api.tooling.nativ.JkNativeCompilation;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkException;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.api.project.JkBuildable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@JkDoc("Creates native images (experimental !)\n" +
        "A native images is an executable file created from Java bytecode.\n" +
        "This KBean allows to create native images from executable jars generated from the project.")
public class NativeKBean extends KBean {

    private Supplier<List<Path>> aotAssetDirs = Collections::emptyList;

    @JkDoc("Extra arguments to pass to native-image compiler.")
    public String args;

    @JkDoc("Tell if the generated executable must by statically linked with native libs")
    public JkNativeCompilation.StaticLink staticLink = JkNativeCompilation.StaticLink.NONE;

    @JkDoc("Use predefined exploratory aot metadata defined in standard repo")
    public boolean useMetadataRepo = true;

    @JkDoc("Use predefined exploratory aot metadata defined in standard repo")
    public String metadataRepoVersion = JkNativeCompilation.DEFAULT_REPO_VERSION;

    @JkDoc("If false, the main class won't be specified in command line arguments. " +
            "This means that it is expected to be mentioned in aot config files.")
    public boolean includeMainClassArg = true;

    @JkDoc("If true, all resources will be included in the native image.")
    public boolean includeAllResources;

    @JkDoc("Creates an native image from the main artifact jar of the project.\n" +
            "If no artifact found, a build is triggered by invoking 'JkProject.packaging.createFatJar(mainArtifactPath)'.")
    public void compile() {
        JkBuildable buildable = getRunbase().findBuildable();
        if (buildable != null) {
            buildable.compileIfNeeded();
            build(buildable);
        } else {
            throw new JkException("No project found in Runbase. Native compilation not yet implemented for base kbean.");
        }

    }

    /**
     * Sets the directories containing ahead-of-time compilation assets for the native image.
     *
     * @param aotAssetDirs A supplier providing a list of paths to the AOT assets directories.
     */
    public NativeKBean setAotAssetDirs(Supplier<List<Path>> aotAssetDirs) {
        this.aotAssetDirs = aotAssetDirs;
        return this;
    }


    public JkNativeCompilation nativeImage(JkBuildable buildable) {
        List<Path> classpath = new LinkedList<>();
        classpath.add(buildable.getClassDir());
        final List<Path> depsAsFiles;
        final Set<JkCoordinate> depsAsCoordinates;
        if (this.useMetadataRepo) {
            JkResolveResult resolveResult = buildable.resolveRuntimeDependencies();
            depsAsFiles = resolveResult.getFiles().getEntries();
            depsAsCoordinates = resolveResult.getDependencyTree().getDescendantModuleCoordinates();
        } else {
            depsAsFiles = buildable.getRuntimeDependenciesAsFiles();
            depsAsCoordinates = null;
        }
        classpath.addAll(depsAsFiles);
        classpath.addAll(0, this.aotAssetDirs.get());
        JkNativeCompilation nativeCompilation = JkNativeCompilation.ofClasspath(classpath);
        if (!JkUtilsString.isBlank(this.args)) {
            nativeCompilation.addExtraParams(JkUtilsString.parseCommandline(this.args));
        }
        if (this.includeMainClassArg) {
            nativeCompilation.setMainClass(buildable.getMainClass());
        }
        nativeCompilation.setIncludesAllResources(this.includeAllResources);
        nativeCompilation.setStaticLinkage(staticLink);
        nativeCompilation.reachabilityMetadata
                .setUseRepo(useMetadataRepo)
                .setRepoVersion(metadataRepoVersion)
                .setDependencies(depsAsCoordinates)
                .setDownloadRepos(buildable.getDependencyResolver().getRepos())
                .setExtractDir(buildable.getOutputDir().resolve("aot-discovery-metadata-repo"));
        return nativeCompilation;
    }

    private void build(JkBuildable buildable) {
        JkNativeCompilation nativeCompilation = nativeImage(buildable);
        String pathString = buildable.getMainJarPath().toString();
        pathString = JkUtilsString.substringBeforeLast(pathString, ".jar");
        nativeCompilation.make(Paths.get(pathString.replace('\\', '/')));
    }

}

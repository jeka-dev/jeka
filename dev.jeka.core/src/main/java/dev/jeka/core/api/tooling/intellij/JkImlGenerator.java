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

package dev.jeka.core.api.tooling.intellij;

import dev.jeka.core.api.depmanagement.JkCoordinateDependency;
import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkQualifiedDependencySet;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.resolution.JkResolutionParameters;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import dev.jeka.core.api.depmanagement.resolution.JkResolvedDependencyNode;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.project.JkIdeSupport;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkConstants;
import dev.jeka.core.tool.JkRunbase;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Provides method to generate and read Eclipse metadata files.
 */
public final class JkImlGenerator {

    private JkPathSequence jekaSrcClasspath = JkPathSequence.of();

    private JkPathSequence jekaSrcImportedProjects = JkPathSequence.of();

    // These are the dependencies for the runbase.
    // This is used only to download sources, as for bin jar, we relieve on runbase classpath.
    private JkDependencySet runbaseDependencies = JkDependencySet.of();

    /* When true, path will be mentioned with $JEKA_HOME$ and $JEKA_REPO$ instead of explicit absolute path. */
    private boolean useVarPath = true;

    private Path explicitJekaHome;

    private Supplier<JkIdeSupport> ideSupportSupplier;

    // Only used if ideSupport is <code>null</code>
    private Path baseDir;

    private boolean excludeJekaLib;

    private Consumer<JkIml> imlConfigurer = jkIml -> {};

    private boolean failOnDepsResolutionError = true;

    private boolean downloadSources = true;

    private JkImlGenerator() {
    }

    public static JkImlGenerator of() {
        return new JkImlGenerator();
    }

    /**
     * Returns the .iml file path for a module located at the specified root dir.
     * @param moduleRootDir The path of module root dir
     */
    public static Path getImlFilePath(Path moduleRootDir) {
        String fileName = moduleRootDir.getFileName().toString().equals("")
                ? moduleRootDir.toAbsolutePath().getFileName().toString()
                : moduleRootDir.getFileName().toString();
        return JkImlGenerator.findExistingImlFile(moduleRootDir)
                .orElse(moduleRootDir.resolve(".idea").resolve(fileName + ".iml"));
    }

    public boolean isUseVarPath() {
        return useVarPath;
    }

    public JkImlGenerator setUseVarPath(boolean useVarPath) {
        this.useVarPath = useVarPath;
        return this;
    }

    public JkImlGenerator setDownloadSources(boolean downloadSources) {
        this.downloadSources = downloadSources;
        return this;
    }

    public JkImlGenerator setJekaSrcClasspath(JkPathSequence jekaSrcClasspath) {
        this.jekaSrcClasspath = jekaSrcClasspath;
        return this;
    }

    /**
     * Only needed to download sources declared od jeka-src dependencies
     */
    public JkImlGenerator setRunbaseDependencies(JkDependencySet runbaseDependencies) {
        this.runbaseDependencies = runbaseDependencies;
        return this;
    }

    public JkImlGenerator setJekaSrcImportedProjects(JkPathSequence jekaSrcImportedProjects) {
        JkUtilsAssert.argument(jekaSrcImportedProjects != null, "defImportedProjects cannot be null.");
        this.jekaSrcImportedProjects = jekaSrcImportedProjects;
        return this;
    }

    public JkImlGenerator setBaseDir(Path baseDir) {
        this.baseDir = baseDir;
        return this;
    }

    public JkImlGenerator configureIml(Consumer<JkIml> imlConfigurer) {
        this.imlConfigurer = this.imlConfigurer.andThen(imlConfigurer);
        return this;
    }

    public Path getExplicitJekaHome() {
        return explicitJekaHome;
    }

    public JkImlGenerator setExplicitJekaHome(Path explicitJekaHome) {
        this.explicitJekaHome = explicitJekaHome;
        return this;
    }

    public JkIdeSupport getIdeSupport() {
        return ideSupportSupplier.get();
    }

    public JkImlGenerator setIdeSupport(Supplier<JkIdeSupport> ideSupportSupplier) {
        this.ideSupportSupplier = ideSupportSupplier;
        return this;
    }

    public JkImlGenerator setIdeSupport(JkIdeSupport ideSupport) {
        this.ideSupportSupplier = () -> ideSupport;
        return this;
    }

    public JkImlGenerator setExcludeJekaLib(boolean excludeJekaLib) {
        this.excludeJekaLib = excludeJekaLib;
        return this;
    }

    public JkImlGenerator setFailOnDepsResolutionError(boolean failOnDepsResolutionError) {
        this.failOnDepsResolutionError = failOnDepsResolutionError;
        return this;
    }

    public JkIml computeIml() {
        return computeIml(false);
    }

    public JkIml computeIml(boolean isForJekaSrc) {
        JkIdeSupport ideSupport = ideSupportSupplier == null ? null : ideSupportSupplier.get();
        Path dir = ideSupport == null ? baseDir : ideSupport.getProdLayout().getBaseDir();
        JkIml iml = JkIml.of().setModuleDir(dir);
        iml.setIsForJekaSrc(isForJekaSrc);

        JkLog.verbose("Compute iml jeka-src classpath : %s", jekaSrcClasspath);
        JkLog.verbose("Compute iml jeka-src imported projects : %s", jekaSrcImportedProjects);
        if (this.useVarPath) {
            iml.pathUrlResolver.setPathSubstitute(JkLocator.getCacheDir());
        }
        iml.component
                .getContent()
                    .addJekaStandards();
        if (ideSupport != null) {
            List<Path> sourcePaths = ideSupport.getProdLayout().resolveSources().getRootDirsOrZipFiles();
            sourcePaths
                    .forEach(path -> iml.component.getContent().addSourceFolder(path, false, null));
            ideSupport.getProdLayout().resolveResources().getRootDirsOrZipFiles().stream()
                    .filter(path -> !sourcePaths.contains(path))
                    .forEach(path -> iml.component.getContent().addSourceFolder(path, false, "java-resource"));
            List<Path> testSourcePaths = ideSupport.getTestLayout().resolveSources().getRootDirsOrZipFiles();
            testSourcePaths
                    .forEach(path -> iml.component.getContent().addSourceFolder(path, true, null));
            ideSupport.getTestLayout().resolveResources().getRootDirsOrZipFiles().stream()
                    .filter(path -> !testSourcePaths.contains(path))
                    .forEach(path -> iml.component.getContent().addSourceFolder(path, false, "java-test-resource"));
            JkDependencyResolver depResolver = ideSupport.getDependencyResolver();
            JkResolutionParameters resolutionParameters = depResolver.getDefaultParams().copy()
                    .setFailOnDependencyResolutionError(failOnDepsResolutionError);
            JkResolveResult resolveResult = depResolver.resolve(ideSupport.getDependencies(), resolutionParameters);
            JkResolvedDependencyNode tree =resolveResult.getDependencyTree();

            if (downloadSources) {
                downloadSources(depResolver, resolveResult);
            }

            JkLog.verbose("Dependencies resolved");
            iml.component.getOrderEntries().addAll(projectOrderEntries(tree));  // too slow
            for (Path path : ideSupport.getGeneratedSourceDirs()) {
                iml.component.getContent().addSourceFolder(path, false, null);
            }
        }
        iml.component.getOrderEntries().addAll(jekaSrcOrderEntries());
        imlConfigurer.accept(iml);
        JkLog.verbose("Iml object generated");
        return iml;
    }

    private void downloadSources(JkDependencyResolver depResolver, JkResolveResult resolveResult) {
        JkLog.info("Download sources");

        JkResolutionParameters resolutionParameters = depResolver.getDefaultParams().copy()
                .setFailOnDependencyResolutionError(false);

        // resolve runbase dependencies
        JkDependencySet runbaseCSourceDeps = JkDependencySet.of();
        if (this.runbaseDependencies.hasModules()) {
            JkResolveResult runbaseResolve = depResolver.resolve(runbaseDependencies, resolutionParameters);
            runbaseCSourceDeps = collectSourceDeps(runbaseResolve);
        }

        JkDependencySet regularSourceDeps = collectSourceDeps(resolveResult);

        depResolver.resolve(runbaseCSourceDeps.and(regularSourceDeps), resolutionParameters);
    }

    private static JkDependencySet collectSourceDeps(JkResolveResult resolveResult) {
        List<JkCoordinateDependency> deps = resolveResult.getDependencyTree().getDescendantModuleCoordinates().stream()
                .map(jkCoordinate -> jkCoordinate.withClassifiers("sources"))
                .map(JkCoordinateDependency::of)
                .collect(Collectors.toList());
        return JkDependencySet.of(deps);
    }

    private Path baseDir() {
        return ideSupportSupplier == null ? baseDir : ideSupportSupplier.get().getProdLayout().getBaseDir();
    }

    // For def, we have computed classpath from runtime
    private List<JkIml.OrderEntry> jekaSrcOrderEntries() {
        OrderEntries orderEntries = new OrderEntries();
        JkPathSequence importedClasspath = jekaSrcImportedProjects.getEntries().stream()
                        .map(JkRunbase::get)
                        .map(JkRunbase::getClasspath)
                        .reduce(JkPathSequence.of(), (ps1, ps2) -> ps1.and(ps2))
                        .withoutDuplicates();


        jekaSrcClasspath.and(jekaSrcImportedProjects).getEntries().stream()
                .filter(path -> !importedClasspath.getEntries().contains(path))
                .filter(path -> !excludeJekaLib || !JkLocator.getJekaJarPath().equals(path))
                .filter(path -> path.toString().contains("..") || !path.endsWith(JkConstants.JEKA_SRC_CLASSES_DIR))  // does not include the work jeka-src classes of this project
                .forEach(path -> orderEntries.add(path, JkIml.Scope.TEST));
        return new LinkedList<>(orderEntries.orderEntries);
    }

    private class OrderEntries {

        private final LinkedHashSet<JkIml.OrderEntry> orderEntries = new LinkedHashSet<>();

        private final Map<Path, JkIml.ModuleLibraryOrderEntry> cache = new HashMap<>();

        void add(Path entry, JkIml.Scope scope) {
            JkIml.OrderEntry orderEntry;
            if (Files.isDirectory(entry)) {
                orderEntry = dirAsOrderEntry(entry, scope);
            } else {
                orderEntry = cache.computeIfAbsent(entry, JkImlGenerator::libAsOrderEntry)
                        .copy()
                        .setScope(scope)
                        .setExported(true);
            }
            orderEntries.add(orderEntry);
        }
    }

    private Path jekaHome() {
        if (explicitJekaHome != null) {
            return explicitJekaHome;
        }
        return JkLocator.getJekaHomeDir();
    }

    private JkIml.OrderEntry dirAsOrderEntry(Path dir, JkIml.Scope scope) {

        // first, look if it is inside an imported project
        for (Path importedProject : this.jekaSrcImportedProjects) {
            if (importedProject.resolve(JkConstants.JEKA_SRC_CLASSES_DIR).equals(dir)) {
                return JkIml.ModuleOrderEntry.of()
                        .setModuleName(moduleName(dir))
                        .setExported(true)
                        .setScope(scope);
            }
        }
        if (Files.exists(dir.resolve("jeka")) && Files.isDirectory(dir.resolve("jeka"))) {
            return JkIml.ModuleOrderEntry.of().setModuleName(moduleName(dir)).setScope(scope).setExported(true);
        }
        return JkIml.ModuleLibraryOrderEntry.of().setClasses(dir).setScope(scope).setExported(true);
    }

    private static String moduleName(Path projectDir) {
        return findExistingImlFile(projectDir)
                .map(path -> JkUtilsString.substringBeforeLast(path.getFileName().toString(), ".iml"))
                .orElse(projectDir.getFileName().toString());
    }

    private static Optional<Path> findExistingImlFile(Path projectDir) {
        return JkPathTree.of(projectDir).andMatching(".idea/*.iml", "*.iml").stream().findFirst();
    }

    private static JkIml.ModuleLibraryOrderEntry libAsOrderEntry(Path libPath) {
        return JkIml.ModuleLibraryOrderEntry.of()
                .setClasses(libPath)
                .setSources(lookForSources(libPath))
                .setJavadoc(lookForJavadoc(libPath));
    }

    private static Path lookForSources(Path binary) {
        final String name = binary.getFileName().toString();
        final String nameWithoutExt = JkUtilsString.substringBeforeLast(name, ".");
        final String ext = JkUtilsString.substringAfterLast(name, ".");
        final String sourceName = nameWithoutExt + "-sources." + ext;
        final List<Path> folders = JkUtilsIterable.listOf(
                binary.resolve("..").normalize(),
                binary.resolve("../sources").normalize(),
                binary.resolve("../../sources").normalize());
        final List<String> names = JkUtilsIterable.listOf(sourceName, nameWithoutExt + "-sources.zip");
        return lookFileHere(folders, names);
    }

    private static Path lookForJavadoc(Path binary) {
        final String name = binary.getFileName().toString();
        final String nameWithoutExt = JkUtilsString.substringBeforeLast(name, ".");
        final String ext = JkUtilsString.substringAfterLast(name, ".");
        final String sourceName = nameWithoutExt + "-javadoc." + ext;
        final List<Path> folders = JkUtilsIterable.listOf(
                binary.resolve("..").normalize(),
                binary.resolve("../../../libs-javadoc").normalize(),
                binary.resolve("../../libs-javadoc").normalize(),
                binary.resolve("../libs-javadoc").normalize());
        final List<String> names = JkUtilsIterable.listOf(sourceName, nameWithoutExt + "-javadoc.zip");
        return lookFileHere(folders, names);
    }

    private static Path lookFileHere(Iterable<Path> folders, Iterable<String> names) {
        for (final Path folder : folders) {
            for (final String name : names) {
                final Path candidate = folder.resolve(name).normalize();
                if (Files.exists(candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private List<JkIml.OrderEntry> projectOrderEntries(JkResolvedDependencyNode tree) {
        long t0 = System.currentTimeMillis();
        OrderEntries orderEntries = new OrderEntries();
        for (final JkResolvedDependencyNode node : tree.toFlattenList()) {

            // Maven dependency
            if (node.isModuleNode()) {
                JkResolvedDependencyNode.JkModuleNodeInfo moduleNodeInfo = node.getModuleInfo();
                if (moduleNodeInfo.isEvicted()) {
                    continue;
                }
                JkIml.Scope scope = ideScope(moduleNodeInfo.getRootConfigurations());
                moduleNodeInfo.getFiles().forEach(path -> orderEntries.add(path, scope));

                // File dependencies (file system + computed)
            } else {
                final JkResolvedDependencyNode.JkFileNodeInfo fileNodeInfo = (JkResolvedDependencyNode.JkFileNodeInfo) node.getNodeInfo();
                JkIml.Scope scope = ideScope(fileNodeInfo.getDeclaredConfigurations());
                if (fileNodeInfo.isComputed()) {
                    final Path projectDir = fileNodeInfo.computationOrigin().getIdeProjectDir();
                    if (projectDir != null) {
                        orderEntries.add(projectDir, scope);
                    }
                } else {
                    fileNodeInfo.getFiles().forEach(path -> orderEntries.add(path, scope));
                }
            }
        }
        JkLog.verbose("projectOrderEntries() took '%s ms", System.currentTimeMillis() - t0);
        return new LinkedList(orderEntries.orderEntries);
    }

    private static JkIml.Scope ideScope(Set<String> scopes) {
        if (scopes.contains(JkQualifiedDependencySet.COMPILE_SCOPE))  {
            return JkIml.Scope.COMPILE;
        } else if (scopes.contains(JkQualifiedDependencySet.RUNTIME_SCOPE)) {
            return JkIml.Scope.RUNTIME;
        } else if (scopes.contains(JkQualifiedDependencySet.PROVIDED_SCOPE)) {
            return JkIml.Scope.PROVIDED;
        } else if (scopes.contains(JkQualifiedDependencySet.TEST_SCOPE)) {
            return JkIml.Scope.TEST;
        } else {
            return JkIml.Scope.COMPILE;
        }
    }

    private static boolean isExistingProject(Path baseDir) {
        return Files.exists(baseDir.resolve(JkConstants.JEKA_SRC_DIR)) ||
                Files.exists(baseDir.resolve(JkConstants.PROPERTIES_FILE));
    }

}

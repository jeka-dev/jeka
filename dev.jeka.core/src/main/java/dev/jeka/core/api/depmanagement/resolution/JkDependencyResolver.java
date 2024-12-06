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

package dev.jeka.core.api.depmanagement.resolution;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkConsoleSpinner;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.tool.JkConstants;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static dev.jeka.core.api.utils.JkUtilsString.pluralize;

/**
 * Class to resolve dependencies to files or dependency tree. Resolution is made upon binary repositories.
 *
 * This cache results at 2 levels
 * <ul>
 *     <li>In Memory : stores the complete result tree in memory to not compute twice the same dependencies in a run.</li>
 *     <li>On Filesystem : stores the path sequence result on file system and persist to one run to another.</li>
 * </ul>
 *
 * @author Jerome Angibaud
 */
public final class JkDependencyResolver  {

    public final JkResolutionParameters parameters;

    // Not necessary but may help Ivy to do some caching ???
    private JkCoordinate moduleHolder;

    private JkRepoSet repos = JkRepoSet.of();

    private final Map<JkQualifiedDependencySet, JkResolveResult> cachedResults = new HashMap<>();

    private boolean useInMemoryCache;

    private boolean useFileSystemCache;

    private boolean displaySpinner;

    private Path fileSystemCacheDir = Paths.get(JkConstants.JEKA_WORK_PATH).resolve("dep-cache");

    private JkDependencyResolver() {
        parameters = JkResolutionParameters.of();
    }

    /**
     * Creates an empty (without repo) dependency resolver fetching module dependencies.
     */
    public static JkDependencyResolver of() {
        return new JkDependencyResolver();
    }

    /**
     * Creates a JkDependencyResolver using the specified JkRepoSet.
     */
    public static JkDependencyResolver of(JkRepoSet repos) {
        return of().setRepos(repos);
    }

    /**
     * Creates a JkDependencyResolver using the specified JkRepo.
     */
    public static JkDependencyResolver of(JkRepo repo) {
        return of().setRepos(repo.toSet());
    }

    /**
     * Returns the repositories the resolution is made on.
     */
    public JkRepoSet getRepos() {
        return this.repos;
    }

    /**
     * Sets the repositories for the dependency resolver.
     */
    public JkDependencyResolver  setRepos(JkRepoSet repos) {
        JkUtilsAssert.argument(repos != null, "repos cannot be null");
        this.repos = repos;
        this.cachedResults.clear();
        return this;
    }

    /**
     * Adds the given repositories to the dependency resolver.
     */
    public JkDependencyResolver  addRepos(JkRepoSet repos) {
        return setRepos(this.repos.and(repos));
    }

    /**
     * Adds the given repositories to the dependency resolver.
     */
    public JkDependencyResolver  addRepos(JkRepo... repos) {
        return addRepos(JkRepoSet.of(Arrays.asList(repos)));
    }

    /**
     * Sets whether to use an in-memory cache for dependency resolution.
     */
    public JkDependencyResolver setUseInMemoryCache(boolean useInMemoryCache) {
        this.useInMemoryCache = useInMemoryCache;
        return this;
    }

    /**
     * Sets whether to use a file system cache for this dependency resolver.
     */
    public JkDependencyResolver setUseFileSystemCache(boolean useFileSystemCache) {
        this.useFileSystemCache = useFileSystemCache;
        return this;
    }

    /**
     * Sets the file system cache directory for this dependency resolver.
     */
    public JkDependencyResolver setFileSystemCacheDir(Path cacheDir) {
        this.fileSystemCacheDir = cacheDir;
        return this;
    }

    /**
     * Sets whether to display a spinner during the resolution process.
     */
    public JkDependencyResolver setDisplaySpinner(boolean displaySpinner) {
        this.displaySpinner = displaySpinner;
        return this;
    }

    /**
     * Returns whether the in-memory cache is used.se.
     */
    public boolean isUseInMemoryCache() {
        return this.useInMemoryCache;
    }

    /**
     * Returns a boolean indicating whether the file system cache is used by the dependency resolver.
     */
    public boolean isUseFileSystemCache() {
        return useFileSystemCache;
    }

    /**
     * Removes all cached results and deletes the contents of the file system cache directory, if it exists.
     */
    public JkDependencyResolver cleanCache() {
        this.cachedResults.clear();
        if (Files.exists(fileSystemCacheDir)) {
            JkPathTree.of(fileSystemCacheDir).deleteContent();
        }
        return this;
    }

    /**
     * Returns the parameters of this dependency resolver.
     */
    public JkResolutionParameters getDefaultParams() {
        return this.parameters;
    }

    /**
     * The underlying dependency manager can cache the resolution on file system
     * for faster result. To make this caching possible, you must set the
     * module+version for which the resolution is made. This is only relevant
     * for of dependencies and have no effect for of dependencies.
     */
    public JkDependencyResolver setModuleHolder(JkCoordinate versionedModule) {
        this.moduleHolder = versionedModule;
        return this;
    }

    /**
     * Resolves the specified JkCoordinateDependency by calling the resolve method with it,
     * using the default resolution parameters of this dependency resolver.
     */
    public JkResolveResult resolve(JkCoordinateDependency coordinateDependency) {
        return resolve(coordinateDependency, parameters);
    }

    /**
     * Resolves the specified dependency by converting it
     * to JkCoordinateDependency and calling the resolve method with it.
     */
    public JkResolveResult resolve(@JkDepSuggest String coordinate) {
        return resolve(JkCoordinateDependency.of(coordinate));
    }

    /**
     * Resolves the specified coordinate dependency using the given resolution parameters.
     */
    public JkResolveResult resolve(JkCoordinateDependency coordinateDependency, JkResolutionParameters params) {
        return resolve(JkDependencySet.of(coordinateDependency), params);
    }

    /**
     * Resolves the specified dependencies and returns the resolve result with default params.
     */
    public JkResolveResult resolve(JkDependencySet dependencies) {
        return resolve(dependencies, parameters);
    }

    /**
     * Resolves the specified dependencies with specified resolution params.
     */
    public JkResolveResult resolve(JkDependencySet dependencies, JkResolutionParameters params) {
        return resolve(JkQualifiedDependencySet.of(
                dependencies.normalised(JkCoordinate.ConflictStrategy.FAIL)
                            .mergeLocalProjectExportedDependencies()), params);
    }

    /**
     * Resolves the specified qualified dependencies. The qualification stands for dependency 'scope' or 'configuration'.
     *
     * @param qualifiedDependencies the dependencies to resolve.
     * @return a result consisting in a dependency tree for modules and a set of files for non-module.
     */
    public JkResolveResult resolve(JkQualifiedDependencySet qualifiedDependencies, JkResolutionParameters params) {
        if (qualifiedDependencies.getEntries().isEmpty()) {
            return JkResolveResult.ofRoot(moduleHolder);
        }
        if (useInMemoryCache) {
            JkResolveResult result = cachedResults.get(qualifiedDependencies);
            if (result != null) {
                return result;
            }
        }
        AtomicReference<JkResolveResult> result = new AtomicReference<>();
        if (displaySpinner) {
            JkConsoleSpinner.of("Resolve dependencies")
                    .setAlternativeMassage("Resolve dependencies ...")
                    .run(() -> result.set(doResolve(qualifiedDependencies, params)));
            return result.get();
        }
        return doResolve(qualifiedDependencies, params);
    }

    private JkResolveResult doResolve(JkQualifiedDependencySet qualifiedDependencies, JkResolutionParameters params) {
        JkQualifiedDependencySet bomResolvedDependencies = replacePomDependencyByVersionProvider(qualifiedDependencies);
        List<JkDependency> allDependencies = bomResolvedDependencies.getDependencies();
        JkQualifiedDependencySet moduleQualifiedDependencies = bomResolvedDependencies
                .withCoordinateDependenciesOnly()
                .withResolvedBoms(effectiveRepos())
                .assertNoUnspecifiedVersion()
                .toResolvedModuleVersions();
        boolean hasModule = !moduleQualifiedDependencies.getDependencies().isEmpty();
        if (effectiveRepos().getRepos().isEmpty() && hasModule) {
            JkLog.warn("You are trying to resolve dependencies on zero repository. Won't be possible to resolve modules.");
        }
        JkInternalDependencyResolver internalDepResolver = JkInternalDependencyResolver.of(effectiveRepos());
        String message = bomResolvedDependencies.getEntries().size() == 1 ?
                "Resolve " + bomResolvedDependencies.getDependencies().get(0).toString()
                : "Resolve " + bomResolvedDependencies.getEntries().size() + " declared dependencies";
        JkLog.verboseStartTask(message);
        JkResolveResult resolveResult;
        if (hasModule) {
            JkUtilsAssert.state(!effectiveRepos().getRepos().isEmpty(), "Cannot resolve module dependency cause no " +
                    "repos has defined on resolver " + this);
            resolveResult = internalDepResolver.resolve(moduleHolder, moduleQualifiedDependencies, params);
        } else {
            resolveResult = JkResolveResult.ofRoot(moduleHolder);
        }
        final JkResolvedDependencyNode mergedNode = resolveResult.getDependencyTree().mergeNonModules(
                allDependencies);
        resolveResult = JkResolveResult.of(mergedNode, resolveResult.getErrorReport());

        if (JkLog.isVerbose()) {
            int moduleCount = resolveResult.getInvolvedCoordinates().size();
            int fileCount = resolveResult.getFiles().getEntries().size();
            JkLog.verbose("  " + pluralize(moduleCount, "coordinate") + " resolved to " + pluralize(fileCount, "file"));
            resolveResult.getFiles().forEach(path -> JkLog.info("  " + path.toString()));
        }
        JkResolveResult.JkErrorReport report = resolveResult.getErrorReport();
        if (report.hasErrors()) {
            if (params.isFailOnDependencyResolutionError()) {
                String msg = report + " \nRepositories = " + effectiveRepos();
                throw new IllegalStateException(msg);
            }
            JkLog.warn(report.toString());
        }
        JkLog.verboseEndTask();
        if (useInMemoryCache) {
            this.cachedResults.put(bomResolvedDependencies, resolveResult);
        }
        return resolveResult;
    }

    /**
     * Resolves the specified qualified dependencies and returns a sequence of resolved files.
     * <p>
     * This methods can leverage of direct resolution caching, while <code>resolve</code> methods can not.
     */
    public List<Path> resolveFiles(
            JkQualifiedDependencySet qualifiedDependencies,
            JkResolutionParameters params) {

        if (!useFileSystemCache) {
            return resolve(qualifiedDependencies, params).getFiles().getEntries();
        }

        Path cacheFile = this.fileSystemCacheDir.resolve(qualifiedDependencies.md5() + ".txt");
        boolean nonExistingEntryOnFs = false;
        if (Files.exists(cacheFile)) {
            if (JkLog.isDebug()) {
                JkLog.info("Resolving %n%s", qualifiedDependencies.toStringMultiline("  "));
                JkLog.info("Found cached resolve-classpath file %s", cacheFile);
            }
            JkPathSequence cachedPathSequence =
                    JkPathSequence.ofPathString(JkPathFile.of(cacheFile).readAsString());
            String cachedCpMsg = cachedPathSequence.getEntries().isEmpty() ? "[empty]" :
                    "\n" + cachedPathSequence.toPathMultiLine("  ");
            JkLog.debug("Cached resolved classpath :%s", cachedCpMsg);
            if (!cachedPathSequence.hasNonExisting()) {
                return cachedPathSequence.getEntries();
            } else {
                nonExistingEntryOnFs = true;
                JkLog.debug(150, "Cached resolved-classpath %s has non existing entries on local file system " +
                        ": need resolving %s", cacheFile, qualifiedDependencies);
            }
        }
        if (!nonExistingEntryOnFs) {
            JkLog.debug(150, "Cached resolved-classpath %s not found : need resolving %s", cacheFile,
                    qualifiedDependencies);
        }
        JkPathSequence result =  this.resolve(qualifiedDependencies, params).getFiles();
        JkLog.debug("Creating resolved-classpath %s for storing dep resolution.", cacheFile);
        JkUtilsPath.createFileSafely(cacheFile);  // On Jeka-ide JkPathFile.createifNotExist throw a "file already exist exception"
        JkPathFile.of(cacheFile).write(result.toPath());
        return result.getEntries();
    }

    /**
     * Resolves the specified qualified dependencies and returns a sequence of resolved files.
     * <p>
     * This methods can leverage of direct resolution caching, while <code>resolve</code> methods can not.
     */
    public List<Path> resolveFiles(
            JkDependencySet dependencies,
            JkResolutionParameters params) {
        return resolveFiles(JkQualifiedDependencySet.of(
                dependencies.normalised(JkCoordinate.ConflictStrategy.FAIL)
                        .mergeLocalProjectExportedDependencies()), params);
    }

    /**
     * Resolves the specified dependencies and returns a sequence of resolved files.
     */
    public List<Path> resolveFiles(JkDependencySet dependencies) {
        return resolveFiles(dependencies, this.parameters);
    }

    /**
     * Resolves the specified coordinate and returns a sequence of resolved files.
     */
    public List<Path> resolveFiles(String coordinate) {
        return resolveFiles(JkDependencySet.of(coordinate), this.parameters);
    }

    /**
     * Returns an alphabetical sorted list of groupId present in these repositories
     */
    public List<String> searchGroups() {
        return JkInternalDependencyResolver.of(effectiveRepos()).searchGroups();
    }

    /**
     * Searches for dependencies based on the specified criteria.
     */
    public List<String> search(String groupCriteria, String moduleNameCriteria, String versionCriteria) {
        return JkInternalDependencyResolver.of(effectiveRepos()).search(groupCriteria, moduleNameCriteria, versionCriteria);
    }

    /**
     * Returns an alphabetical sorted list of module ids present in these repositories for the specified groupId.
     */
    public List<String> searchModuleIds(String groupId) {
        return JkInternalDependencyResolver.of(effectiveRepos()).searchModules(groupId);
    }

    /**
     * Returns an alphabetical sorted list of version present in these repositories for the specified moduleId.
     */
    public List<String> searchVersions(JkModuleId jkModuleId) {
        return JkInternalDependencyResolver.of(effectiveRepos()).searchVersions(jkModuleId).stream()
                .sorted(JkVersion.VERSION_COMPARATOR).collect(Collectors.toList());
    }

    /**
     * Returns an alphabetical sorted list of version present in these repositories for the specified moduleId.
     */
    public List<String> searchVersions(String moduleId) {
        return searchVersions(JkModuleId.of(moduleId));
    }


    @Override
    public String toString() {
        if (repos == null) {
            return "No repo resolver";
        }
        return repos.toString();
    }

    private JkRepoSet effectiveRepos() {
        boolean includeLocalRepo = true;
        return includeLocalRepo ? repos.and(JkRepo.ofLocal()) : repos;
    }

    private JkQualifiedDependencySet replacePomDependencyByVersionProvider(JkQualifiedDependencySet dependencySet) {
        List<JkQualifiedDependency> dependencies = dependencySet.getEntries();
        JkQualifiedDependencySet result = dependencySet;
        for (JkQualifiedDependency qualifiedDependency : dependencies) {
            JkDependency dependency = qualifiedDependency.getDependency();
            if (dependency instanceof JkCoordinateDependency) {
                JkCoordinateDependency coordinateDependency = (JkCoordinateDependency) dependency;
                JkCoordinate coordinate = coordinateDependency.getCoordinate();
                JkCoordinate.JkArtifactSpecification spec = coordinate.getArtifactSpecification();

                // This is dependency on a pom, meaning that we are supposed to use this as a BOM
                // Therefore, we transform the dependency to such an entry in version provider
                if (spec.getClassifier() == null && "pom".equals(spec.getType())) {
                    JkVersionProvider versionProvider = result.getVersionProvider().andBom(coordinate);
                    result = result.withVersionProvider(versionProvider);
                    result = result.remove(qualifiedDependency);
                }
            }
        }
        JkVersionProvider versionProvider = result.getVersionProvider();
        return result.withVersionProvider(versionProvider.withResolvedBoms(this.repos));
    }

}

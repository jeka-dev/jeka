package dev.jeka.core.api.depmanagement.resolution;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.JkModuleId;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsAssert;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static dev.jeka.core.api.utils.JkUtilsString.pluralize;

/**
 * Class to resolve dependencies to files or dependency tree. Resolution is made upon binary repositories.
 *
 * @author Jerome Angibaud
 */
public final class JkDependencyResolver<T> {

    public final JkResolutionParameters<JkDependencyResolver<T> > parameters;

    // Not necessary but may help Ivy to do some caching ???
    private JkCoordinate moduleHolder;

    private JkRepoSet repos = JkRepoSet.of();

    private final Map<JkQualifiedDependencySet, JkResolveResult> cachedResults = new HashMap<>();

    private boolean useCache;

    /**
     * For parent chaining
     */
    public final T __;

    private JkDependencyResolver(T parent) {
        __ = parent;
        parameters = JkResolutionParameters.ofParent(this);
    }

    /**
     * Creates an empty (without repo) dependency resolver fetching module dependencies.
     */
    public static JkDependencyResolver<Void> of() {
        return ofParent(null);
    }

    /**
     * Same as {@link #of()} but providing parent chaining.
     */
    public static <T> JkDependencyResolver<T> ofParent(T parent) {
        return new JkDependencyResolver(parent);
    }

    /**
     * Returns the repositories the resolution is made on.
     */
    public JkRepoSet getRepos() {
        return this.repos;
    }

    public JkDependencyResolver<T> setRepos(JkRepoSet repos) {
        JkUtilsAssert.argument(repos != null, "repos cannot be null");
        this.repos = repos;
        this.cachedResults.clear();
        return this;
    }

    public JkDependencyResolver<T> addRepos(JkRepoSet repos) {
        return setRepos(this.repos.and(repos));
    }

    public JkDependencyResolver<T> addRepos(JkRepo... repos) {
        return addRepos(JkRepoSet.of(Arrays.asList(repos)));
    }

    public JkDependencyResolver<T> setUseCache(boolean useCache) {
        this.useCache = useCache;
        return this;
    }

    public boolean isUseCache() {
        return this.useCache;
    }

    public JkDependencyResolver<T> cleanCache() {
        this.cachedResults.clear();
        return this;
    }

    /**
     * Returns the parameters of this dependency resolver.
     */
    public JkResolutionParameters<JkDependencyResolver<T> > getDefaultParams() {
        return this.parameters;
    }

    /**
     * The underlying dependency manager can cache the resolution on file system
     * for faster result. To make this caching possible, you must set the
     * module+version for which the resolution is made. This is only relevant
     * for of dependencies and have no effect for of dependencies.
     */
    public JkDependencyResolver<T> setModuleHolder(JkCoordinate versionedModule) {
        this.moduleHolder = versionedModule;
        return this;
    }

    public JkResolveResult resolve(JkCoordinateDependency coordinateDependency) {
        return resolve(coordinateDependency, parameters);
    }

    public JkResolveResult resolve(String dependencyDescription) {
        return resolve(JkCoordinateDependency.of(dependencyDescription));
    }

    public JkResolveResult resolve(JkCoordinateDependency coordinateDependency, JkResolutionParameters params) {
        return resolve(JkDependencySet.of(coordinateDependency), params);
    }

    public JkResolveResult resolve(JkDependencySet dependencies) {
        return resolve(dependencies, parameters);
    }

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
        if (useCache) {
            JkResolveResult result = cachedResults.get(qualifiedDependencies);
            if (result != null) {
                return result;
            }
        }
        List<JkDependency> allDependencies = qualifiedDependencies.getDependencies();
        JkQualifiedDependencySet moduleQualifiedDependencies = qualifiedDependencies
                .withModuleDependenciesOnly()
                .withResolvedBoms(this.repos)
                .assertNoUnspecifiedVersion()
                .toResolvedModuleVersions();
        boolean hasModule = !moduleQualifiedDependencies.getDependencies().isEmpty();
        if (repos.getRepos().isEmpty() && hasModule) {
            JkLog.warn("You are trying to resolve dependencies on zero repository. Won't be possible to resolve modules.");
        }
        JkInternalDependencyResolver internalDepResolver = JkInternalDependencyResolver.of(this.repos);
        String message = qualifiedDependencies.getEntries().size() == 1 ?
                "Resolve " + qualifiedDependencies.getDependencies().get(0).toString()
                : "Resolve " + qualifiedDependencies.getEntries().size() + " declared dependencies";
        JkLog.startTask(message);
        JkResolveResult resolveResult;
        if (hasModule) {
            JkUtilsAssert.state(!repos.getRepos().isEmpty(), "Cannot resolve module dependency cause no " +
                    "repos has defined on resolver " + this);
            resolveResult = internalDepResolver.resolve(moduleHolder, moduleQualifiedDependencies, params);
        } else {
            resolveResult = JkResolveResult.ofRoot(moduleHolder);
        }
        final JkResolvedDependencyNode mergedNode = resolveResult.getDependencyTree().mergeNonModules(
                allDependencies);
        resolveResult = JkResolveResult.of(mergedNode, resolveResult.getErrorReport());
        int moduleCount = resolveResult.getInvolvedCoordinates().size();
        int fileCount = resolveResult.getFiles().getEntries().size();
        JkLog.info(pluralize(moduleCount, "module") + " -> " + pluralize(fileCount, "file"));
        if (JkLog.isVerbose()) {
            resolveResult.getFiles().forEach(path -> JkLog.info(path.toString()));
        }
        JkResolveResult.JkErrorReport report = resolveResult.getErrorReport();
        if (report.hasErrors()) {
            if (params.isFailOnDependencyResolutionError()) {
                String msg = report.toString() + " \nRepositories = " + repos;
                throw new IllegalStateException(msg);
            }
            JkLog.warn(report.toString());
        }
        JkLog.endTask();
        if (useCache) {
            this.cachedResults.put(qualifiedDependencies, resolveResult);
        }
        return resolveResult;
    }

    /**
     * Returns an alphabetical sorted list of groupId present in these repositories
     */
    public List<String> searchGroups() {
        return JkInternalDependencyResolver.of(this.repos).searchGroups();
    }

    public List<String> search(String groupCriteria, String moduleNameCriteria, String versionCriteria) {
        return JkInternalDependencyResolver.of(this.repos).search(groupCriteria, moduleNameCriteria, versionCriteria);
    }

    /**
     * Returns an alphabetical sorted list of module ids present in these repositories for the specified groupId.
     */
    public List<String> searchModuleIds(String groupId) {
        return JkInternalDependencyResolver.of(this.repos).searchModules(groupId);
    }

    /**
     * Returns an alphabetical sorted list of version present in these repositories for the specified moduleId.
     */
    public List<String> searchVersions(JkModuleId jkModuleId) {
        return JkInternalDependencyResolver.of(this.repos).searchVersions(jkModuleId).stream()
                .sorted(JkVersion.VERSION_COMPARATOR).collect(Collectors.toList());
    }

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


}

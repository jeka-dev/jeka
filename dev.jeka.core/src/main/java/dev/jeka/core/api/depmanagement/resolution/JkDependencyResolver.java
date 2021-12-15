package dev.jeka.core.api.depmanagement.resolution;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsAssert;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static dev.jeka.core.api.utils.JkUtilsString.plurialize;

/**
 * Class to resolve dependencies to files or dependency tree. Resolution is made upon binary repositories.
 *
 * @author Jerome Angibaud
 */
public final class JkDependencyResolver<T> {

    private final JkResolutionParameters<JkDependencyResolver<T> > defaultParameters;

    // Not necessary but helps Ivy to hide data efficiently.
    private JkVersionedModule moduleHolder;

    private JkRepoSet repos = JkRepoSet.of();

    private final Map<JkQualifiedDependencySet, JkResolveResult> cachedResults = new HashMap<>();

    private boolean useCache;

    /**
     * For parent chaining
     */
    public final T __;

    private JkDependencyResolver(T parent) {
        __ = parent;
        defaultParameters = JkResolutionParameters.ofParent(this);
    }

    /**
     * Creates a empty (without repo) dependency resolver fetching module dependencies.
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

    public boolean isUseCache(boolean useCache) {
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
        return this.defaultParameters;
    }

    /**
     * The underlying dependency manager can cache the resolution on file system
     * for faster result. To make this caching possible, you must set the
     * module+version for which the resolution is made. This is only relevant
     * for of dependencies and have no effect for of dependencies.
     */
    public JkDependencyResolver<T> setModuleHolder(JkVersionedModule versionedModule) {
        this.moduleHolder = versionedModule;
        return this;
    }

    public JkResolveResult resolve(JkModuleDependency moduleDependency) {
        return resolve(moduleDependency, defaultParameters);
    }

    public JkResolveResult resolve(JkModuleDependency moduleDependency, JkResolutionParameters params) {
        return resolve(JkDependencySet.of(moduleDependency), params);
    }

    public JkResolveResult resolve(JkDependencySet dependencies) {
        return resolve(dependencies, defaultParameters);
    }

    public JkResolveResult resolve(JkDependencySet dependencies, JkResolutionParameters params) {
        return resolve(JkQualifiedDependencySet.of(
                dependencies.normalised(JkVersionedModule.ConflictStrategy.FAIL)
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
        JkQualifiedDependencySet moduleQualifiedDependencies = qualifiedDependencies.withModuleDependenciesOnly()
                .replaceUnspecifiedVersionsWithProvider().assertNoUnspecifiedVersion();
        boolean hasModule = !moduleQualifiedDependencies.getDependencies().isEmpty();
        if (repos.getRepos().isEmpty() && hasModule) {
            JkLog.warn("You are trying to resolve dependencies on zero repository. Won't be possible to resolve modules.");
        }
        JkInternalDependencyResolver internalDepResolver = JkInternalDependencyResolver.of(this.repos);
        JkLog.startTask("Resolve dependencies (" + qualifiedDependencies.getEntries().size() + " declared dependencies).");
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
        if (JkLog.verbosity() == JkLog.Verbosity.VERBOSE) {
            JkLog.info(plurialize(resolveResult.getInvolvedModules().size(), "module")
                    + resolveResult.getInvolvedModules());
            JkLog.info(plurialize(resolveResult.getFiles().getEntries().size(), "artifact") + ".");
        } else {
            JkLog.info(plurialize(resolveResult.getInvolvedModules().size(), "module") + " : " +
                    plurialize(resolveResult.getFiles().getEntries().size(), "file") + ".");
        }
        JkResolveResult.JkErrorReport report = resolveResult.getErrorReport();
        if (report.hasErrors()) {
            if (params.isFailOnDependencyResolutionError()) {
                throw new IllegalStateException(report.toString());
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

    /**
     * Returns an alphabetical sorted list of module ids present in these repositories for the specified groupId.
     */
    public List<String> searchModules(String groupId) {
        return JkInternalDependencyResolver.of(this.repos).searchModules(groupId);
    }

    /**
     * Returns an alphabetical sorted list of version present in these repositories for the specified moduleId.
     */
    public List<String> searchVersions(JkModuleId moduleId) {
        return JkInternalDependencyResolver.of(this.repos).searchVersions(moduleId).stream()
                .sorted(JkVersion.VERSION_COMPARATOR).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        if (repos == null) {
            return "No repo resolver";
        }
        return repos.toString();
    }

}

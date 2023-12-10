package dev.jeka.core.api.depmanagement.resolution;

import dev.jeka.core.api.depmanagement.*;
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
public final class JkDependencyResolver  {

    public final JkResolutionParameters parameters;

    // Not necessary but may help Ivy to do some caching ???
    private JkCoordinate moduleHolder;

    private JkRepoSet repos = JkRepoSet.of();

    private final Map<JkQualifiedDependencySet, JkResolveResult> cachedResults = new HashMap<>();

    private boolean useCache;

    private final boolean includeLocalRepo = true;

    private JkDependencyResolver() {
        parameters = JkResolutionParameters.of();
    }

    /**
     * Creates an empty (without repo) dependency resolver fetching module dependencies.
     */
    public static JkDependencyResolver of() {
        return new JkDependencyResolver();
    }

    public static JkDependencyResolver of(JkRepoSet repos) {
        return of().setRepos(repos);
    }

    public static JkDependencyResolver of(JkRepo repo) {
        return of().setRepos(repo.toSet());
    }

    /**
     * Returns the repositories the resolution is made on.
     */
    public JkRepoSet getRepos() {
        return this.repos;
    }

    public JkDependencyResolver  setRepos(JkRepoSet repos) {
        JkUtilsAssert.argument(repos != null, "repos cannot be null");
        this.repos = repos;
        this.cachedResults.clear();
        return this;
    }

    public JkDependencyResolver  addRepos(JkRepoSet repos) {
        return setRepos(this.repos.and(repos));
    }

    public JkDependencyResolver  addRepos(JkRepo... repos) {
        return addRepos(JkRepoSet.of(Arrays.asList(repos)));
    }

    public JkDependencyResolver  setUseCache(boolean useCache) {
        this.useCache = useCache;
        return this;
    }

    public boolean isUseCache() {
        return this.useCache;
    }

    public JkDependencyResolver cleanCache() {
        this.cachedResults.clear();
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
        JkQualifiedDependencySet bomResolvedDependencies = replacePomDependencyByVersionProvider(qualifiedDependencies);
        List<JkDependency> allDependencies = bomResolvedDependencies.getDependencies();
        JkQualifiedDependencySet moduleQualifiedDependencies = bomResolvedDependencies
                .withModuleDependenciesOnly()
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
        JkLog.startTask(message);
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
        int moduleCount = resolveResult.getInvolvedCoordinates().size();
        int fileCount = resolveResult.getFiles().getEntries().size();
        JkLog.info(pluralize(moduleCount, "module") + " -> " + pluralize(fileCount, "file"));
        if (JkLog.isVerbose()) {
            resolveResult.getFiles().forEach(path -> JkLog.info(path.toString()));
        }
        JkResolveResult.JkErrorReport report = resolveResult.getErrorReport();
        if (report.hasErrors()) {
            if (params.isFailOnDependencyResolutionError()) {
                String msg = report + " \nRepositories = " + effectiveRepos();
                throw new IllegalStateException(msg);
            }
            JkLog.warn(report.toString());
        }
        JkLog.endTask();
        if (useCache) {
            this.cachedResults.put(bomResolvedDependencies, resolveResult);
        }
        return resolveResult;
    }

    /**
     * Returns an alphabetical sorted list of groupId present in these repositories
     */
    public List<String> searchGroups() {
        return JkInternalDependencyResolver.of(effectiveRepos()).searchGroups();
    }

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

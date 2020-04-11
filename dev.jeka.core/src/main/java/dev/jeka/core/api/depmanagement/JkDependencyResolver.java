package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsTime;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static dev.jeka.core.api.utils.JkUtilsString.plurialize;

/**
 * Class to resolve dependencies to files or dependency tree. Resolution is made upon binary repositories.
 *
 * @author Jerome Angibaud
 */
public final class JkDependencyResolver<T> {

    private final JkResolutionParameters<JkDependencyResolver<T> > parameters;

    // Not necessary but helps Ivy to hide data efficiently.
    private JkVersionedModule moduleHolder;

    private JkRepoSet repos = JkRepoSet.of();

    /**
     * For parent chaining
     */
    public final T __;

    private JkDependencyResolver(T parent) {
        __ = parent;
        parameters = JkResolutionParameters.ofParent(this);
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
        JkUtilsAssert.notNull(repos, "repos cannot be null");
        this.repos = repos;
        return this;
    }

    public JkDependencyResolver<T> addRepos(JkRepoSet repos) {
        return setRepos(this.repos.and(repos));
    }

    public JkDependencyResolver<T> addRepos(JkRepo ... repos) {
        return addRepos(JkRepoSet.of(Arrays.asList(repos)));
    }

    /**
     * Returns the parameters of this dependency resolver.
     */
    public JkResolutionParameters<JkDependencyResolver<T> > getParams() {
        return this.parameters;
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

    /**
     * @see JkDependencyResolver#resolve(JkDependencySet, JkScope...)
     */
    public JkResolveResult resolve(JkDependencySet dependencies, Iterable<JkScope> scopes) {
        return resolve(dependencies, JkUtilsIterable.arrayOf(scopes, JkScope.class));
    }

    /**
     * Resolves the specified dependencies (dependencies declared as module) for the specified scopes.
     * @param dependencies the dependencies to resolve.
     * @param scopes scope for resolution (compile, runtime, ...). If no scope is specified, then it is resolved for all scopes.
     * @return a result consisting in a dependency tree for modules and a set of files for non-module.
     */
    public JkResolveResult resolve(JkDependencySet dependencies, JkScope ... scopes) {
        if (repos.getRepoList().isEmpty()) {
            JkLog.warn("You are trying to resolve dependencies on no repositories. This will fail.");
        }
        JkInternalDepResolver internalDepResolver = JkInternalDepResolver.of(this.repos);
        JkLog.trace("Preparing to resolve dependencies for module " + moduleHolder);
        long start = System.nanoTime();
        final String msg = scopes.length == 0 ? "Resolving dependencies " :
                "Resolving dependencies with specified scopes " + Arrays.asList(scopes);
        JkLog.startTask(msg);
        JkResolveResult resolveResult = !dependencies.hasModules() ? JkResolveResult.ofRoot(moduleHolder) :
                internalDepResolver.resolve(moduleHolder, dependencies.withModulesOnly(), parameters, scopes);
        final JkDependencyNode mergedNode = resolveResult.getDependencyTree().mergeNonModules(dependencies,
                    JkUtilsIterable.setOf(scopes));
        resolveResult = JkResolveResult.of(mergedNode, resolveResult.getErrorReport());
        if (JkLog.verbosity() == JkLog.Verbosity.VERBOSE) {
            JkLog.info(plurialize(resolveResult.getInvolvedModules().size(), "module")
                    + resolveResult.getInvolvedModules());
            JkLog.info(plurialize(resolveResult.getFiles().getEntries().size(), "artifact") + ".");
        } else {
            JkLog.info(plurialize(resolveResult.getInvolvedModules().size(), "module") + " resolved to " +
                    plurialize(resolveResult.getFiles().getEntries().size(), "artifact file") + ".");
        }
        JkLog.endTask("Done in " + JkUtilsTime.durationInMillis(start) + " milliseconds.");
        return resolveResult;
    }

    /**
     * Returns an alphabetical sorted list of groupId present in these repositories
     */
    public List<String> searchGroups() {
        return JkInternalDepResolver.of(this.repos).searchGroups();
    }

    /**
     * Returns an alphabetical sorted list of module ids present in these repositories for the specified groupId.
     */
    public List<String> searchModules(String groupId) {
        return JkInternalDepResolver.of(this.repos).searchModules(groupId);
    }

    /**
     * Returns an alphabetical sorted list of version present in these repositories for the specified moduleId.
     */
    public List<String> searchVersions(JkModuleId moduleId) {
        return JkInternalDepResolver.of(this.repos).searchVersions(moduleId).stream()
                .sorted(JkVersion.SEMANTIC_COMARATOR).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        if (repos == null) {
            return "No repo resolver";
        }
        return repos.toString();
    }

}

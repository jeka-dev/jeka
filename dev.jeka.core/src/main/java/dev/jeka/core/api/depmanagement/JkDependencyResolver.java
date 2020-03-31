package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsTime;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static dev.jeka.core.api.utils.JkUtilsString.plurialize;

/**
 * Class to resolve dependencies to files or dependency tree. Resolution is made upon binary repositories.
 *
 * @author Jerome Angibaud
 */
public final class JkDependencyResolver {

    private final JkInternalDepResolver jkInternalModuleDepResolver;

    private final JkResolutionParameters parameters;

    // Not necessary but helps Ivy to hide data efficiently.
    private final JkVersionedModule module;

    private final JkRepoSet repos;

    private final Path baseDir;

    private JkDependencyResolver(JkInternalDepResolver jkInternalModuleDepResolver,
                                 JkVersionedModule module, JkResolutionParameters resolutionParameters, JkRepoSet repos, Path baseDir) {
        this.jkInternalModuleDepResolver = jkInternalModuleDepResolver;
        this.module = module;
        this.parameters = resolutionParameters;
        this.repos = repos;
        this.baseDir = baseDir;
    }

    /**
     * Creates a dependency resolver fetching module dependencies in the specified repos. If
     * the specified JkRepo contains no {@link JkRepo} then the created.
     */
    public static JkDependencyResolver of(JkRepoSet repos) {
        final JkInternalDepResolver ivyResolver = JkInternalDepResolver.of(repos);
        return new JkDependencyResolver(ivyResolver,  null, JkResolutionParameters.of(), repos, Paths.get(""));
    }

    /**
     * @See {@link #of(JkRepoSet)}
     */
    public static JkDependencyResolver of(JkRepo repo1, JkRepo... repos) {
        return of(JkRepoSet.of(repo1, repos));
    }

    public static JkDependencyResolver of() {
        return of(JkRepoSet.of());
    }

    /**
     * Returns the repositories the resolution is made on.
     */
    public JkRepoSet getRepos() {
        return this.repos;
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
        JkLog.trace("Preparing to resolve dependencies for module " + module);
        long start = System.nanoTime();
        final String msg = scopes.length == 0 ? "Resolving dependencies " :
                "Resolving dependencies with specified scopes " + Arrays.asList(scopes);
        JkLog.startTask(msg);
        JkResolveResult resolveResult = (jkInternalModuleDepResolver == null || !dependencies.hasModules()) ? JkResolveResult.ofRoot(module) :
                jkInternalModuleDepResolver.resolve(module, dependencies.withModulesOnly(),
                    parameters, scopes);
        final JkDependencyNode mergedNode = resolveResult.getDependencyTree().mergeNonModules(dependencies,
                    JkUtilsIterable.setOf(scopes));
        resolveResult = JkResolveResult.of(mergedNode, resolveResult.getErrorReport()).withBaseDir(baseDir);
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
     * The underlying dependency manager can cache the resolution on file system
     * for faster result. To make this caching possible, you must set the
     * module+version for which the resolution is made. This is only relevant
     * for of dependencies and have no effect for of dependencies.
     */
    public JkDependencyResolver withModuleHolder(JkVersionedModule versionedModule) {
        return new JkDependencyResolver(this.jkInternalModuleDepResolver, versionedModule,
                this.parameters, this.repos, this.baseDir);
    }

    /**
     * Returns an dependency resolver identical to this one but with the specified repositories.
     */
    public JkDependencyResolver withRepos(JkRepoSet otherRepos) {
        return new JkDependencyResolver(JkInternalDepResolver.of(otherRepos), this.module,
                this.parameters, otherRepos, this.baseDir);
    }

    /**
     * @see #withRepos(JkRepoSet)
     */
    public JkDependencyResolver withRepos(JkRepo repo, JkRepo... otherRepos) {
        return withRepos(JkRepoSet.of(repo, otherRepos));
    }

    /**
     * @see #withRepos(JkRepoSet)
     */
    public JkDependencyResolver andRepos(JkRepoSet repoSet) {
        return withRepos(this.repos.and(repoSet));
    }

    /**
     * Returns an dependency resolver identical to this one but with the specified repositories.
     */
    public JkDependencyResolver withParams(JkResolutionParameters params) {
        return new JkDependencyResolver(this.jkInternalModuleDepResolver, this.module,
                params, this.repos, this.baseDir);
    }

    /**
     * Returns an dependency resolver identical to this one but with specified the base directory. The base directory is used
     * to resolve relative files to absolute files for {@link JkFileSystemDependency}. This directory is used by
     * {@link #resolve(JkDependencySet, JkScope...)} method as it returns only absolute files.
     */
    public JkDependencyResolver withBasedir(Path baseDir) {
        return new JkDependencyResolver(this.jkInternalModuleDepResolver, this.module,
                this.parameters, this.repos, baseDir);
    }

    /**
     * Returns the parameters of this dependency resolver.
     */
    public JkResolutionParameters getParams() {
        return this.parameters;
    }

    /**
     * Returns an alphabetical sorted list of groupId present in these repositories
     */
    public List<String> searchGroups() {
        return this.jkInternalModuleDepResolver.searchGroups();
    }

    /**
     * Returns an alphabetical sorted list of module ids present in these repositories for the specified groupId.
     */
    public List<String> searchModules(String groupId) {
        return this.jkInternalModuleDepResolver.searchModules(groupId);
    }

    /**
     * Returns an alphabetical sorted list of version present in these repositories for the specified moduleId.
     */
    public List<String> searchVersions(JkModuleId moduleId) {
        return this.jkInternalModuleDepResolver.searchVersions(moduleId).stream()
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

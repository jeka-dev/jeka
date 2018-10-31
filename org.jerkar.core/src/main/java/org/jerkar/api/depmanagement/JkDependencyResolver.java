package org.jerkar.api.depmanagement;

import static org.jerkar.api.utils.JkUtilsString.plurialize;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.jerkar.api.file.JkPathSequence;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsTime;

/**
 * Class to resolve dependencies to files or dependency tree. Resolution is made upon binary repositories.
 *
 * @author Jerome Angibaud
 */
public final class JkDependencyResolver {

    /**
     * @See {@link #of(JkRepoSet)}
     */
    public static JkDependencyResolver of(JkRepo... repos) {
        return of(JkRepoSet.of(repos));
    }

    private final InternalDepResolver internalResolver;

    private final JkResolutionParameters parameters;

    // Not necessary but nice if present in order to let Ivy hide data
    // efficiently.
    private final JkVersionedModule module;

    private final JkRepoSet repos;

    private final Path baseDir;

    private JkDependencyResolver(InternalDepResolver internalResolver,
            JkVersionedModule module, JkResolutionParameters resolutionParameters, JkRepoSet repos, Path baseDir) {
        this.internalResolver = internalResolver;
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
        if (repos.getRepoList().isEmpty()) {
            return new JkDependencyResolver(null, null, null, JkRepoSet.ofEmpty(), Paths.get(""));
        }
        final InternalDepResolver ivyResolver = InternalDepResolvers.ivy(repos);
        return new JkDependencyResolver(ivyResolver,  null, null, repos, Paths.get(""));
    }

    /**
     * @see JkDependencyResolver#resolve(JkDependencySet, JkScope...)
     */
    public JkResolveResult resolve(JkDependencySet dependencies, Iterable<JkScope> scopes) {
        return resolve(dependencies, JkUtilsIterable.arrayOf(scopes, JkScope.class));
    }

    /**
     * Returns the repositories the resolution is made on.
     */
    public JkRepoSet getRepos() {
        return this.repos;
    }

    /**
     * Gets the path containing all the resolved dependencies as artifact files
     * for the specified scopes.
     * <p>
     * If no scope is specified then return all file dependencies and the
     * dependencies specified. About the of dependency the same rule than
     * for {@link #resolve(JkDependencySet, JkScope...)} apply.
     * </p>
     * The result is ordered according the order dependencies has been declared.
     * About ordering of transitive dependencies, they come after the explicit ones and
     * the dependee of the first explicitly declared dependency come before the dependee
     * of the second one and so on.
     * @throws IllegalStateException if the resolution has not been achieved successfully
     */
    public JkPathSequence fetch(JkDependencySet dependencies, JkScope... scopes) {
        if (internalResolver != null && dependencies.hasModules()) {
            JkResolveResult resolveResult = resolve(dependencies, scopes).assertNoError();
            return JkPathSequence.ofMany(resolveResult.getDependencyTree().getAllResolvedFiles()).withoutDuplicates();
        }
        final List<Path> result = new LinkedList<>();
        for (final JkScopedDependency scopedDependency : dependencies) {
            if (scopedDependency.isInvolvedInAnyOf(scopes) || scopes.length == 0) {
                final JkDependency dependency = scopedDependency.withDependency();
                final JkFileDependency fileDependency = (JkFileDependency) dependency;
                result.addAll(fileDependency.getFiles());
            }
        }
        return JkPathSequence.ofMany(result).withoutDuplicates().resolveTo(baseDir);
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
        JkResolveResult resolveResult = internalResolver == null ? JkResolveResult.ofRoot(module) :
                internalResolver.resolve(module, dependencies.withModulesOnly(),
                    parameters, scopes);
        final JkDependencyNode mergedNode = resolveResult.getDependencyTree().mergeNonModules(dependencies,
                    JkUtilsIterable.setOf(scopes));
        resolveResult = JkResolveResult.of(mergedNode, resolveResult.getErrorReport());
        if (JkLog.verbosity() == JkLog.Verbosity.VERBOSE) {
            JkLog.info(plurialize(resolveResult.getInvolvedModules().size(), "module")
                    + resolveResult.getInvolvedModules());
            JkLog.info(plurialize(resolveResult.getLocalFiles().size(), "artifact") + ".");
        } else {
            JkLog.info(plurialize(resolveResult.getInvolvedModules().size(), "module") + " leading to " +
                    plurialize(resolveResult.getLocalFiles().size(), "artifact") + ".");
        }
        JkLog.endTask("Done in " + JkUtilsTime.durationInMillis(start) + " milliseconds.");
        return resolveResult;
    }

    /**
     * The underlying dependency manager can cache the resolution on file ofSystem
     * for faster result. To make this caching possible, you must set the
     * module+version for which the resolution is made. This is only relevant
     * for of dependencies and have no effect for of dependencies.
     */
    public JkDependencyResolver withModuleHolder(JkVersionedModule versionedModule) {
        return new JkDependencyResolver(this.internalResolver, versionedModule,
                this.parameters, this.repos, this.baseDir);
    }

    /**
     * Returns an dependency resolver identical to this one but with the specified repositories.
     */
    public JkDependencyResolver withRepos(JkRepoSet otherRepos) {
        return new JkDependencyResolver(InternalDepResolvers.ivy(otherRepos), this.module,
                this.parameters, otherRepos, this.baseDir);
    }

    /**
     * @see #withRepos(JkRepoSet)
     */
    public JkDependencyResolver withRepos(JkRepo... otherRepos) {
        return withRepos(JkRepoSet.of(otherRepos));
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
        return new JkDependencyResolver(this.internalResolver, this.module,
                params, this.repos, this.baseDir);
    }

    /**
     * Returns an dependency resolver identical to this one but with the base directory. The base directory is used
     * to resolve relative files to absolute files. This directory is used by
     * {@link #fetch(JkDependencySet, JkScope...)} method as it returns only absolute files.
     */
    public JkDependencyResolver withBasedir(JkResolutionParameters params) {
        return new JkDependencyResolver(this.internalResolver, this.module,
                params, this.repos, this.baseDir);
    }

    /**
     * Returns the parameters of this dependency resolver.
     */
    public JkResolutionParameters getParams() {
        return this.parameters;
    }

    @Override
    public String toString() {
        if (repos == null) {
            return "No repo resolver";
        }
        return repos.toString();
    }

}

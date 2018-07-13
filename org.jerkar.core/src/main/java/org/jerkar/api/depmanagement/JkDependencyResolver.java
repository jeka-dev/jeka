package org.jerkar.api.depmanagement;

import static org.jerkar.api.utils.JkUtilsString.plurialize;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.jerkar.api.file.JkPathSequence;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsTime;

/**
 * A resolver for a given set of dependency. Each instance of
 * <code>JkDependencyResolver</code> defines the dependencies to resolve, this
 * means that you must instantiate one for each dependency set you want to
 * resolve. <br/>
 * Each instance of <code>JkDependencyResolver</code> keep in cache resolution
 * setting so a resolution o a given scope is never computed twice.
 *
 * The result of the resolution depends on the parameters you have set on it.
 * See {@link JkResolutionParameters}
 *
 * @author Jerome Angibaud
 */
public final class JkDependencyResolver {

    /**
     * Creates a dependency resolver fetching module dependencies in the specified repos. If
     * the specified JkRepo contains no {@link JkRepo} then the created.
     */
    public static JkDependencyResolver of(JkRepoSet repos) {
        if (repos.list().isEmpty()) {
            return new JkDependencyResolver(null, null, null, JkRepoSet.empty());
        }
        final InternalDepResolver ivyResolver = InternalDepResolvers.ivy(repos);
        return new JkDependencyResolver(ivyResolver,  null, null, repos);
    }

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

    private JkDependencyResolver(InternalDepResolver internalResolver,
            JkVersionedModule module, JkResolutionParameters resolutionParameters, JkRepoSet repos) {
        this.internalResolver = internalResolver;
        this.module = module;
        this.parameters = resolutionParameters;
        this.repos = repos;
    }

    /**
     * @see JkDependencyResolver#resolve(JkDependencySet, JkScope...)
     */
    public JkResolveResult resolve(JkDependencySet dependencies, Iterable<JkScope> scopes) {
        return resolve(dependencies, JkUtilsIterable.arrayOf(scopes, JkScope.class));
    }

    /**
     * Resolves the of dependencies (dependencies declared as external
     * module) for the specified scopes. If no scope is specified, then it is
     * resolved for all scopes.
     */
    public JkResolveResult resolve(JkDependencySet dependencies, JkScope... scopes) {
        if (internalResolver == null) {
            final List<JkDependencyNode> nodes = new LinkedList<>();
            for (final JkScopedDependency scopedDependency : dependencies) {
                nodes.add(JkDependencyNode.ofFileDep((JkFileDependency) scopedDependency.dependency(),
                        scopedDependency.scopes()));
            }
            final JkDependencyNode.ModuleNodeInfo info;
            if (this.module == null) {
                info = JkDependencyNode.ModuleNodeInfo.anonymousRoot();
            } else {
                info = JkDependencyNode.ModuleNodeInfo.root(this.module);
            }
            final JkDependencyNode root = JkDependencyNode.ofModuleDep(info, nodes);
            return JkResolveResult.of(root, JkResolveResult.JkErrorReport.allFine());
        }
        return resolveWithInternalResolver(dependencies, dependencies.versionProvider(), scopes);
    }

    /**
     * Returns the repositories the resolution is made on.
     */
    public JkRepoSet repositories() {
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
    public JkPathSequence get(JkDependencySet dependencies, JkScope... scopes) {
        JkResolveResult resolveResult = null;
        if (internalResolver != null && dependencies.containsModules()) {
            resolveResult = resolveWithInternalResolver(dependencies, dependencies.versionProvider(), scopes).assertNoError();
            return JkPathSequence.ofMany(resolveResult.dependencyTree().allFiles()).withoutDuplicates();
        }
        final List<Path> result = new LinkedList<>();
        for (final JkScopedDependency scopedDependency : dependencies) {
            if (scopedDependency.isInvolvedInAnyOf(scopes) || scopes.length == 0) {
                final JkDependency dependency = scopedDependency.dependency();
                final JkFileDependency fileDependency = (JkFileDependency) dependency;
                result.addAll(fileDependency.paths());
            }
        }
        return JkPathSequence.ofMany(result).withoutDuplicates();
    }

    private JkResolveResult resolveWithInternalResolver(JkDependencySet dependencies, JkVersionProvider transitiveVersionOverride, JkScope ... scopes) {
        JkLog.trace("Preparing to resolve dependencies for module " + module);
        long start = System.nanoTime();
        final String msg = scopes.length == 0 ? "Resolving dependencies " :
                "Resolving dependencies with specified scopes " + Arrays.asList(scopes);
        JkLog.startTask(msg);
        JkResolveResult resolveResult = internalResolver.resolve(module, dependencies.onlyModules(),
                    parameters, transitiveVersionOverride, scopes);
        final JkDependencyNode mergedNode = resolveResult.dependencyTree().mergeNonModules(dependencies,
                    JkUtilsIterable.setOf(scopes));
        resolveResult = JkResolveResult.of(mergedNode, resolveResult.errorReport());
        if (JkLog.verbosity() == JkLog.Verbosity.VERBOSE) {
            JkLog.info(plurialize(resolveResult.involvedModules().size(), "module")
                    + resolveResult.involvedModules());
            JkLog.info(plurialize(resolveResult.localFiles().size(), "artifact") + ".");
        } else {
            JkLog.info(plurialize(resolveResult.involvedModules().size(), "module") + " leading to " +
                    plurialize(resolveResult.localFiles().size(), "artifact") + ".");
        }
        JkLog.endTask("Done in " + JkUtilsTime.durationInMillis(start) + " milliseconds.");
        return resolveResult;
    }

    /**
     * The underlying dependency manager can cache the resolution on file system
     * for faster result. To make this caching possible, you must set the
     * module+projectVersion for which the resolution is made. This is only relevant
     * for of dependencies and have no effect for of dependencies.
     */
    public JkDependencyResolver withModuleHolder(JkVersionedModule versionedModule) {
        return new JkDependencyResolver(this.internalResolver, versionedModule,
                this.parameters, this.repos);
    }

    /**
     * Change the repositories for dependency resolution
     */
    public JkDependencyResolver withRepos(JkRepoSet otherRepos) {
        return new JkDependencyResolver(InternalDepResolvers.ivy(otherRepos), this.module,
                this.parameters, otherRepos);
    }

    /**
     * Change the repositories for dependency resolution
     */
    public JkDependencyResolver withRepos(JkRepo... otherRepos) {
        return withRepos(JkRepoSet.of(otherRepos));
    }

    /**
     * You can alter the resolver behavior through these settings. his is only
     * relevant for of dependencies and have no effect for of
     * dependencies.
     */
    public JkDependencyResolver withParams(JkResolutionParameters params) {
        return new JkDependencyResolver(this.internalResolver, this.module,
                params, this.repos);
    }

    /**
     * Returns the parameters of this dependency resolver.
     */
    public JkResolutionParameters params() {
        return this.parameters;
    }

    @Override
    public String toString() {
        if (repos == null) {
            return "of depenedncy resolver";
        }
        return repos.toString();
    }

}

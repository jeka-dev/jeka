package org.jerkar.api.depmanagement;

import java.io.File;
import java.util.*;

import org.jerkar.api.depmanagement.JkDependency.JkFileDependency;
import org.jerkar.api.file.JkPath;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsIterable;

import static  org.jerkar.api.utils.JkUtilsString.*;

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

    private static final JkScope NULL_SCOPE;

    static {
        NULL_SCOPE = JkScope.of("JkDependencyResolver.NULL_SCOPE");
    }

    /**
     * Creates a dependency resolver relying on a dependency manager. Such a
     * resolver is able to resolve dependencies transitively downloading
     * artifacts hosted on Maven or Ivy repository. If you don't have module
     * dependencies (only local dependencies) then you'd better use
     * {@link #unmanaged(JkDependencies)} instead.
     */
    public static JkDependencyResolver managed(JkRepos repos, JkDependencies dependencies) {
        final InternalDepResolver ivyResolver = InternalDepResolvers.ivy(repos);
        return new JkDependencyResolver(ivyResolver, dependencies, null, null,
                null, repos);
    }

    /**
     * Creates a dependency manager that does not need to rely on a dependency
     * manager. This is a case when you use only local file dependencies.
     */
    public static JkDependencyResolver unmanaged(JkDependencies dependencies) {
        if (dependencies.containsModules()) {
            throw new IllegalArgumentException(
                    "Your dependencies contain a reference to a managed extarnal module."
                            + "Use #managed method factory instead.");
        }
        return new JkDependencyResolver(null, dependencies, null, null, null, JkRepos.of());
    }

    private final InternalDepResolver internalResolver;

    private final JkDependencies dependencies;

    private final JkResolutionParameters parameters;

    // Not necessary but nice if present in order to let Ivy hide data
    // efficiently.
    private final JkVersionedModule module;

    private final JkVersionProvider transitiveVersionOverride;

    private final JkRepos repos;

    private JkDependencyResolver(InternalDepResolver internalResolver, JkDependencies dependencies,
            JkVersionedModule module, JkResolutionParameters resolutionParameters,
            JkVersionProvider transitiveVersionOverride, JkRepos repos) {
        this.internalResolver = internalResolver;
        this.dependencies = dependencies;
        this.module = module;
        this.parameters = resolutionParameters;
        this.transitiveVersionOverride = transitiveVersionOverride == null ? JkVersionProvider.empty() : transitiveVersionOverride;
        this.repos = repos;
    }

    /**
     * Returns the dependencies this resolver has been instantiated with.
     */
    public JkDependencies dependenciesToResolve() {
        return this.dependencies;
    }

    /**
     * @see JkDependencyResolver#resolve(JkScope...)
     */
    public JkResolveResult resolve(Iterable<JkScope> scopes) {
        return resolve(JkUtilsIterable.arrayOf(scopes, JkScope.class));
    }

    /**
     * Resolves the managed dependencies (dependencies declared as external
     * module) for the specified scopes. If no scope is specified, then it is
     * resolved for all scopes.
     */
    public JkResolveResult resolve(JkScope... scopes) {
        if (internalResolver == null) {
            return JkResolveResult.empty();
        }
        return getResolveResult(this.transitiveVersionOverride, scopes);
    }

    /**
     * Returns the repositories the resolution is made on.
     */
    public JkRepos repositories() {
        return this.repos;
    }

    /**
     * Gets the path containing all the resolved dependencies as artifact files
     * for the specified scopes.
     * <p>
     * If no scope is specified then return all file dependencies and the
     * dependencies specified. About the managed dependency the same rule than
     * for {@link #resolve(JkScope...)} apply.
     * </p>
     * The result is ordered according the order {@link #dependencies} has been declared.
     * About ordering of transitive dependencies, they come after the explicit ones and
     * the dependee of the first explicitly declared dependency come before the dependee
     * of the second one and so on.
     * @throws IllegalStateException if the resolution has not been achieved successfully
     */
    public JkPath get(JkScope... scopes) {
        JkResolveResult resolveResult = null;
        if (internalResolver != null && this.dependencies.containsModules()) {
            resolveResult = getResolveResult(this.transitiveVersionOverride, scopes).assertNoError();
        }
        final List<File> result = new LinkedList<File>();
        for (final JkScopedDependency scopedDependency : this.dependencies) {
            if (scopedDependency.isInvolvedInAnyOf(scopes) || scopes.length == 0) {
                final JkDependency dependency = scopedDependency.dependency();
                if (dependency instanceof JkFileDependency) {
                    final JkFileDependency fileDependency = (JkFileDependency) dependency;
                    result.addAll(fileDependency.files());
                } else if (dependency instanceof JkModuleDependency && resolveResult != null) {
                    final JkModuleDependency moduleDependency = (JkModuleDependency) dependency;
                    result.addAll(resolveResult.filesOf(moduleDependency.moduleId()));
                }
            }
        }
        if (resolveResult != null) {
            result.addAll(resolveResult.localFiles());
        }
        return JkPath.of(result).withoutDoubloons();
    }

    private JkResolveResult getResolveResult(JkVersionProvider transitiveVersionOverride, JkScope ... scopes) {
        JkLog.trace("Preparing to resolve dependencies for module " + module);
        JkLog.startln("Resolving dependencies with specified scopes " + Arrays.asList(scopes) + ".");
        JkResolveResult resolveResult = internalResolver.resolve(module, dependencies.onlyModules(),
                    parameters, transitiveVersionOverride, scopes);
        JkDependencyNode mergedNode = resolveResult.dependencyTree().mergeNonModules(dependencies, JkUtilsIterable.setOf(scopes));
        resolveResult = JkResolveResult.of(mergedNode, resolveResult.errorReport());
        if (JkLog.verbose()) {
            JkLog.info(plurialize(resolveResult.involvedModules().size(), "module") + resolveResult.involvedModules());
            JkLog.info(plurialize(resolveResult.localFiles().size(), "artifact") + ".");
        } else {
            JkLog.info(plurialize(resolveResult.involvedModules().size(), "module") + " leading to " +
                    plurialize(resolveResult.localFiles().size(),"artifact") + ".");
        }
        JkLog.done();
        return resolveResult;
    }

    /**
     * Returns <code>true</code> if this resolver does not contain any
     * dependencies.
     */
    public boolean isEmpty() {
        for (final JkScope scope : this.dependencies.declaredScopes()) {
            if (!this.get(scope).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * The underlying dependency manager can cache the resolution on file system
     * for faster result. To make this caching possible, you must set the
     * module+version for which the resolution is made. This is only relevant
     * for managed dependencies and have no effect for unmanaged dependencies.
     */
    public JkDependencyResolver withModuleHolder(JkVersionedModule versionedModule) {
        return new JkDependencyResolver(this.internalResolver, dependencies, versionedModule,
                this.parameters, this.transitiveVersionOverride, this.repos);
    }

    /**
     * Provides a mean to force module versions coming from transitive dependencies.
     */
    public JkDependencyResolver withTransitiveVersionOverride(JkVersionProvider transitiveVersionOverride) {
        return new JkDependencyResolver(this.internalResolver, dependencies, this.module,
                this.parameters, transitiveVersionOverride, this.repos);
    }


    /**
     * You can alter the resolver behavior through these settings. his is only
     * relevant for managed dependencies and have no effect for unmanaged
     * dependencies.
     */
    public JkDependencyResolver withParams(JkResolutionParameters params) {
        return new JkDependencyResolver(this.internalResolver, this.dependencies, this.module,
                params, this.transitiveVersionOverride, this.repos);
    }

    @Override
    public String toString() {
        return dependencies.toString();
    }

}

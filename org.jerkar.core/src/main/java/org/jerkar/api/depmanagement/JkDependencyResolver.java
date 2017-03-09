package org.jerkar.api.depmanagement;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jerkar.api.depmanagement.JkDependency.JkFileDependency;
import org.jerkar.api.file.JkPath;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsIterable;

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

    private static final JkScope NULL_SCOPE = JkScope.of("JkDependencyResolver.NULL_SCOPE");

    private static JkDependencyResolver managed(JkRepos repos, JkDependencies dependencies,
            JkVersionedModule module, JkResolutionParameters resolutionParameters,
            JkVersionProvider transitiveVersionOverride) {
        final InternalDepResolver ivyResolver = InternalDepResolvers.ivy(repos);
        return new JkDependencyResolver(ivyResolver, dependencies, module, resolutionParameters,
                transitiveVersionOverride);
    }

    /**
     * Creates a dependency resolver relying on a dependency manager. Such a
     * resolver is able to resolve dependencies transitively downloading
     * artifacts hosted on Maven or Ivy repository. If you don't have module
     * dependencies (only local dependencies) then you'd better use
     * {@link #unmanaged(JkDependencies)} instead.
     */
    public static JkDependencyResolver managed(JkRepos repos, JkDependencies dependencies) {
        return managed(repos, dependencies, null, JkResolutionParameters.of(), null);
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
        return new JkDependencyResolver(null, dependencies, null, null, null);
    }

    private final Map<JkScope, JkResolveResult> cachedResolveResult = new HashMap<JkScope, JkResolveResult>();

    private final InternalDepResolver internalResolver;

    private final JkDependencies dependencies;

    private final JkResolutionParameters parameters;

    // Not necessary but nice if present in order to let Ivy hide data
    // efficiently.
    private final JkVersionedModule module;

    private final JkVersionProvider transitiveVersionOverride;

    private JkDependencyResolver(InternalDepResolver internalResolver, JkDependencies dependencies,
            JkVersionedModule module, JkResolutionParameters resolutionParameters,
            JkVersionProvider transitiveVersionOverride) {
        this.internalResolver = internalResolver;
        this.dependencies = dependencies;
        this.module = module;
        this.parameters = resolutionParameters;
        this.transitiveVersionOverride = transitiveVersionOverride == null ? JkVersionProvider.empty() : transitiveVersionOverride;
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
            return JkResolveResult.empty(this.module);
        }
        final Set<JkScope> scopesSet = new HashSet<JkScope>();
        for (final JkScope scope : scopes) {
            if (!this.dependencies.involvedScopes().contains(scope)) {
                JkLog.info("No dependency declared with scope '" + scope.name() + "'");
                continue;
            }
            scopesSet.add(scope);
            scopesSet.addAll(scope.ancestorScopes());
        }
        JkResolveResult resolveResult = JkResolveResult.empty(module);
        for (final JkScope scope : scopesSet) {
            resolveResult = resolveResult.and(getResolveResult(scope, this.transitiveVersionOverride));
        }
        if (scopes.length == 0) {
            resolveResult = resolveResult.and(getResolveResult(null, this.transitiveVersionOverride));
        }
        return resolveResult;
    }

    /**
     * Gets artifacts belonging to the same module as the specified ones but
     * having the specified scopes.
     */
    public JkAttachedArtifacts getAttachedArtifacts(Set<JkVersionedModule> modules,
            JkScope... scopes) {
        return internalResolver.getArtifacts(modules, scopes);
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
     */
    public JkPath get(JkScope... scopes) {
        JkResolveResult resolveResult = null;
        if (internalResolver != null && this.dependencies.containsModules()) {
            resolveResult = getResolveResult(scopes);
        }
        final List<File> result = new LinkedList<File>();
        for (final JkScopedDependency scopedDependency : this.dependencies) {
            if (scopedDependency.isInvolvedInAnyOf(scopes) || scopes.length == 0) {
                final JkDependency dependency = scopedDependency.dependency();
                if (dependency instanceof JkFileDependency) {
                    final JkFileDependency fileDependency = (JkFileDependency) dependency;
                    result.addAll(fileDependency.files());
                } else if (dependency instanceof JkModuleDependency) {
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




    private JkResolveResult getResolveResult(JkScope... scopes) {
        if (scopes.length == 0) {
            return this.getResolveResult(null, this.transitiveVersionOverride);
        }
        JkResolveResult result = JkResolveResult.empty(this.module);
        for (final JkScope scope : scopes) {
            result = result.and(this.getResolveResult(scope, this.transitiveVersionOverride));
        }
        return result;
    }

    private JkResolveResult getResolveResult(JkScope scope, JkVersionProvider transitiveVersionOverride) {
        final JkScope cachedScope = scope == null ? NULL_SCOPE : scope;
        final JkResolveResult result = cachedResolveResult.get(cachedScope);
        if (result != null) {
            return result;
        }
        JkLog.trace("Preparing to resolve dependencies for module " + module);
        if (scope != null) {
            JkLog.startln("Resolving dependencies for scope '" + scope.name() + "'");
        } else {
            JkLog.startln("Resolving dependencies without specified scope");
        }

        final JkResolveResult resolveResult;
        if (module != null) {
            resolveResult = internalResolver.resolve(module, dependencies.onlyModules(), scope,
                    parameters, transitiveVersionOverride);
        } else {
            resolveResult = internalResolver.resolveAnonymous(dependencies.onlyModules(), scope,
                    parameters, transitiveVersionOverride);
        }
        cachedResolveResult.put(cachedScope, resolveResult);
        if (JkLog.verbose()) {
            JkLog.info(resolveResult.involvedModules().size() + " module(s): " + resolveResult.involvedModules());
            JkLog.info(resolveResult.localFiles().size() + " artifact(s).");
        } else {
            JkLog.info(resolveResult.involvedModules().size() + " module(s) leading to " +
                    resolveResult.localFiles().size() + " artifact(s).");
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
                this.parameters, this.transitiveVersionOverride);
    }

    /**
     * Provides a mean to force module versions coming from transitive dependencies.
     */
    public JkDependencyResolver withTransitiveVersionOverride(JkVersionProvider transitiveVersionOverride) {
        return new JkDependencyResolver(this.internalResolver, dependencies, this.module,
                this.parameters, transitiveVersionOverride);
    }


    /**
     * You can alter the resolver behavior through these settings. his is only
     * relevant for managed dependencies and have no effect for unmanaged
     * dependencies.
     */
    public JkDependencyResolver withParams(JkResolutionParameters params) {
        return new JkDependencyResolver(this.internalResolver, this.dependencies, this.module,
                params, this.transitiveVersionOverride);
    }

    /**
     * Returns a dependency resolver identical to this one but with the
     * specified dependencies.
     */
    public JkDependencyResolver withDependencies(JkDependencies dependencies) {
        return new JkDependencyResolver(this.internalResolver, dependencies, module, parameters, this.transitiveVersionOverride);
    }

    @Override
    public String toString() {
        return dependencies.toString();
    }

}

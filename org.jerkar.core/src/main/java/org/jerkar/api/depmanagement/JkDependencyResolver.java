package org.jerkar.api.depmanagement;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jerkar.api.file.JkPath;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsIterable;

/**
 * A resolver for a given set of dependency. Each instance of <code>JkDependencyResolver</code> defines the dependencies to resolve, this
 * means that you must instantiate one for each dependency set you want to resolve. <br/>
 * Each instance of <code>JkDependencyResolver</code> keep in cache resolution setting so a resolution o a given scope is never computed twice.
 * 
 * The result of the resolution depends on the parameters you have set on it. See {@link JkResolutionParameters}
 * 
 * @author Jerome Angibaud
 */
public final class JkDependencyResolver  {

	private static final String IVYRESOLVER_CLASS_NAME = IvyResolver.class.getName();

	private static final JkScope SINGLE_SCOPE = JkScope.of("JkDependencyResolver.SINGLE");

	private static final JkScope SINGLE_SCOPE_NON_TRANS = JkScope.of("JkDependencyResolver.SINGLE.NON_TRANS").transitive(false);

	private static final JkScope NULL_SCOPE = JkScope.of("JkDependencyResolver.NULL_SCOPE");


	private static JkDependencyResolver managed(JkRepos repos, JkDependencies dependencies, JkVersionedModule module, JkResolutionParameters resolutionParameters) {

		// Ivy Resolver is loaded in a dedicated classloader containing Ivy.
		// Thus we need to serialize the result to be used in the current class loader.
		final InternalDepResolver ivyResolver = IvyClassloader.CLASSLOADER.transClassloaderProxy(InternalDepResolver.class, IVYRESOLVER_CLASS_NAME, "of", repos);
		return new JkDependencyResolver(ivyResolver, dependencies, module, resolutionParameters);
	}

	/**
	 * Creates a dependency resolver relying on a dependency manager. Such a resolver is able to resolve dependencies
	 * transitively downloading artifacts hosted on Maven or Ivy repository. If you don't have module dependencies (only local dependencies)
	 * then you'd better use {@link #unmanaged(JkDependencies)} instead.
	 */
	public static JkDependencyResolver managed(JkRepos repos, JkDependencies dependencies) {
		return managed(repos, dependencies, null, JkResolutionParameters.of());
	}

	/**
	 * Creates a dependency manager that does not need to rely on a dependency manager.
	 * This is a case when you use only local file dependencies.
	 */
	public static JkDependencyResolver unmanaged(JkDependencies dependencies) {
		if (dependencies.containsModules()) {
			throw new IllegalArgumentException("Your dependencies contain a reference to a managed extarnal module."
					+ "Use #managed method factory instead.");
		}
		return new JkDependencyResolver(null, dependencies, null, null);
	}

	static JkPath get(JkRepos repos, JkModuleDependency dep, boolean transitive) {
		final JkScope scope = transitive ? SINGLE_SCOPE : SINGLE_SCOPE_NON_TRANS;
		final InternalDepResolver resolver = IvyClassloader.CLASSLOADER.transClassloaderProxy(InternalDepResolver.class, IVYRESOLVER_CLASS_NAME, "of", repos);
		final JkScopeMapping scopeMapping = JkScopeMapping.of(scope).to("default");
		final JkResolveResult result = resolver.resolveAnonymous(JkDependencies.on(scope, dep), scope, JkResolutionParameters.of().withDefault(scopeMapping));
		return JkPath.of(result.localFiles());
	}

	private final Map<JkScope, JkResolveResult> cachedResolveResult = new HashMap<JkScope, JkResolveResult>();

	private final InternalDepResolver internalResolver;

	private final JkDependencies dependencies;

	private final JkResolutionParameters parameters;

	// Not necessary but nice if present in order to let Ivy hide data efficiently.
	private final JkVersionedModule module;

	private JkDependencyResolver(InternalDepResolver jkIvyResolver, JkDependencies dependencies, JkVersionedModule module, JkResolutionParameters resolutionParameters) {
		this.internalResolver = jkIvyResolver;
		this.dependencies = dependencies;
		this.module = module;
		this.parameters = resolutionParameters;
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
	 * Resolves the managed dependencies (dependencies declared as external module) for the specified scopes.
	 * If no scope is specified, then it is resolved for all scopes.
	 */
	public JkResolveResult resolve(JkScope ... scopes) {
		if (internalResolver == null) {
			return JkResolveResult.empty();
		}
		final Set<JkScope> scopesSet = new HashSet<JkScope>();
		for (final JkScope scope : scopes) {
			if (!this.dependencies.involvedScopes().contains(scope)) {
				JkLog.warn("No dependencies declared with scope '" + scope.name() + "'");
				continue;
			}
			scopesSet.add(scope);
			scopesSet.addAll(scope.ancestorScopes());
		}
		JkResolveResult resolveResult = JkResolveResult.empty();
		for (final JkScope scope : scopesSet) {
			resolveResult = resolveResult.and(getResolveResult(scope));
		}
		if (scopes.length == 0) {
			resolveResult = resolveResult.and(getResolveResult(null));
		}
		return resolveResult;
	}

	/**
	 * Gets artifacts belonging to the same module as the specified ones but having the specified scopes.
	 */
	public JkAttachedArtifacts getAttachedArtifacts(Set<JkVersionedModule> modules, JkScope ... scopes) {
		return internalResolver.getArtifacts(modules, scopes);
	}


	/**
	 * Gets the path containing all the resolved dependencies as artifact files for the specified scopes.<p/>
	 * 
	 * If no scope is specified then return all file dependencies and the dependencies specified.
	 * About the managed dependency the same rule than for {@link #resolve(JkScope...)} apply.
	 */
	public final JkPath get(JkScope ...scopes) {
		if (scopes.length == 0) {
			return getSingleScope(null);
		}
		JkPath path = JkPath.of();
		for (final JkScope scope : scopes) {
			path = path.and(getSingleScope(scope));
		}
		return path.withoutDoubloons();
	}

	private final JkPath getSingleScope(JkScope scope) {
		final List<File> result = new LinkedList<File>();

		// Add local, non-managed dependencies
		if (scope == null) {
			result.addAll(this.dependencies.allLocalFileDependencies().entries());
		} else {
			result.addAll(this.dependencies.localFileDependencies(scope).entries());
		}
		if (internalResolver == null) {
			return JkPath.of(result);
		}
		result.addAll(this.getResolveResult(scope).localFiles());
		return JkPath.of(result);
	}

	private JkResolveResult getResolveResult(JkScope scope) {
		final JkScope cachedScope = scope == null ? NULL_SCOPE : scope;
		final JkResolveResult result = cachedResolveResult.get(cachedScope);
		if (result != null) {
			return result;
		}
		if (scope != null) {
			JkLog.startln("Resolving dependencies for scope '" + scope.name() + "'");
		} else {
			JkLog.startln("Resolving dependencies without scope");
		}

		final JkResolveResult resolveResult;
		if (module != null) {
			resolveResult = internalResolver.resolve(module, dependencies, scope, parameters);
		} else {
			resolveResult = internalResolver.resolveAnonymous(dependencies, scope, parameters);
		}
		cachedResolveResult.put(cachedScope, resolveResult);
		JkLog.info(resolveResult.involvedModules().size() + " artifacts: " + resolveResult.involvedModules());
		JkLog.done("Resolve result = " + resolveResult);
		return resolveResult;
	}

	/**
	 * Returns <code>true<code> if this resolver does not contain any dependencies.
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
	 * The underlying dependency manager can cache the resolution on file system for faster result.
	 * To make this caching possible, you must set the module+version for which the resolution is made.
	 * This is only relevant for managed dependencies and have no effect for unmanaged dependencies.
	 */
	public JkDependencyResolver withModuleHolder(JkVersionedModule versionedModule) {
		return new JkDependencyResolver(this.internalResolver, dependencies, versionedModule, this.parameters);
	}

	/**
	 * You can alter the resolver behavior through these settings.
	 * his is only relevant for managed dependencies and have no effect for unmanaged dependencies.
	 */
	public JkDependencyResolver withParams(JkResolutionParameters params) {
		return new JkDependencyResolver(this.internalResolver, this.dependencies, this.module, params);
	}

	@Override
	public String toString() {
		return dependencies.toString();
	}

}

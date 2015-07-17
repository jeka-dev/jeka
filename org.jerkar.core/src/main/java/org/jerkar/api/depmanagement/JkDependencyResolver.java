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

public final class JkDependencyResolver  {

	private static final String IVY_CLASS = IvyResolver.class.getName();

	private static final JkScope SINGLE_SCOPE = JkScope.of("JkDependencyResolver.SINGLE");

	private static final JkScope SINGLE_SCOPE_NON_TRANS = JkScope.of("JkDependencyResolver.SINGLE.NON_TRANS").transitive(false);

	/**
	 * Creates a resolver according the specified parameters.
	 * 
	 * @param module The module is needed only to help caching resolution
	 */
	public static JkDependencyResolver managed(JkRepos repos, JkDependencies dependencies, JkVersionedModule module, JkResolutionParameters resolutionParameters) {
		final InternalDepResolver ivyResolver = IvyClassloader.CLASSLOADER.transClassloaderProxy(InternalDepResolver.class, IVY_CLASS, "of", repos);
		return new JkDependencyResolver(ivyResolver, dependencies, module, resolutionParameters);
	}

	public static JkDependencyResolver managed(JkRepos repos, JkDependencies dependencies, JkVersionedModule module) {
		return managed(repos, dependencies, module, JkResolutionParameters.of());
	}


	public static JkDependencyResolver unmanaged(JkDependencies dependencies) {
		if (dependencies.containsExternalModule()) {
			throw new IllegalArgumentException("Your dependencies contain a reference to a managed extarnal module."
					+ "Use #managed method factory instead.");
		}
		return new JkDependencyResolver(null, dependencies, null, null);
	}

	static JkPath get(JkRepos repos, JkExternalModuleDependency dep, boolean transitive) {
		final JkScope scope = transitive ? SINGLE_SCOPE : SINGLE_SCOPE_NON_TRANS;
		final InternalDepResolver resolver = IvyClassloader.CLASSLOADER.transClassloaderProxy(InternalDepResolver.class, IVY_CLASS, "of", repos);
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

	public boolean isManagedDependencyResolver() {
		return this.internalResolver != null;
	}

	public JkDependencies declaredDependencies() {
		return this.dependencies;
	}

	/**
	 * @see JkDependencyResolver#resolveManagedDependencies(JkScope...)
	 */
	public JkResolveResult resolveManagedDependencies(Iterable<JkScope> scopes) {
		return resolveManagedDependencies(JkUtilsIterable.arrayOf(scopes, JkScope.class));
	}

	/**
	 * Resolves the managed dependencies (dependencies declared as external module).
	 */
	public JkResolveResult resolveManagedDependencies(JkScope ... scopes) {
		if (internalResolver == null) {
			return JkResolveResult.empty();
		}
		final Set<JkScope> scopesSet = new HashSet<JkScope>();
		for (final JkScope scope : scopes) {
			scopesSet.add(scope);
			scopesSet.addAll(scope.ancestorScopes());
		}
		JkResolveResult resolveResult = JkResolveResult.empty();
		for (final JkScope scope : scopesSet) {
			resolveResult = resolveResult.and(getResolveResult(scope));
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
	 * Gets the path containing all the artifact files for the specified scopes.
	 */
	public final JkPath get(JkScope ...scopes) {
		JkPath path = JkPath.of();
		for (final JkScope scope : scopes) {
			path = path.and(getSingleScope(scope));
		}
		return path.withoutDoubloons();
	}

	private final JkPath getSingleScope(JkScope scope) {
		final List<File> result = new LinkedList<File>();

		// Add local, non-managed dependencies
		result.addAll(this.dependencies.localFileDependencies(scope).entries());
		if (internalResolver == null) {
			return JkPath.of(result);
		}
		result.addAll(this.getResolveResult(scope).localFiles());
		return JkPath.of(result);
	}

	private JkResolveResult getResolveResult(JkScope scope) {
		final JkResolveResult result = cachedResolveResult.get(scope);
		if (result != null) {
			return result;
		}
		JkLog.startln("Resolving dependencies for scope '" + scope.name() + "'");
		final JkResolveResult resolveResult;
		if (module != null) {
			resolveResult = internalResolver.resolve(module, dependencies, scope, parameters);
		} else {
			resolveResult = internalResolver.resolveAnonymous(dependencies, scope, parameters);
		}
		cachedResolveResult.put(scope, resolveResult);
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

	@Override
	public String toString() {
		return dependencies.toString();
	}

}

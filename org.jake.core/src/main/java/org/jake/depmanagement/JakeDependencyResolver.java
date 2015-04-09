package org.jake.depmanagement;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jake.JakeLog;
import org.jake.JakePath;
import org.jake.depmanagement.ivy.JakeIvy;
import org.jake.depmanagement.ivy.JakeIvy.AttachedArtifacts;

public final class JakeDependencyResolver  {

	public static JakeDependencyResolver managed(JakeIvy jakeIvy, JakeDependencies dependencies, JakeVersionedModule module, JakeResolutionParameters resolutionParameters) {
		return new JakeDependencyResolver(jakeIvy, dependencies, module, resolutionParameters);
	}

	public static JakeDependencyResolver unmanaged(JakeDependencies dependencies) {
		if (dependencies.containsExternalModule()) {
			throw new IllegalArgumentException("Your dependencies contain a reference to a managed extarnal module."
					+ "Use #managed method factory instead.");
		}
		return new JakeDependencyResolver(null, dependencies, null, null);
	}

	private final Map<JakeScope, JakePath> cachedDeps = new HashMap<JakeScope, JakePath>();

	private final JakeIvy jakeIvy;

	private final JakeDependencies dependencies;

	private final JakeResolutionParameters parameters;

	// Not necessary but nice if present in order to let Ivy hide data efficiently.
	private final JakeVersionedModule module;

	private JakeDependencyResolver(JakeIvy jakeIvy, JakeDependencies dependencies, JakeVersionedModule module, JakeResolutionParameters resolutionParameters) {
		this.jakeIvy = jakeIvy;
		this.dependencies = dependencies;
		this.module = module;
		this.parameters = resolutionParameters;
	}

	private List<File> getDeclaredDependencies(JakeScope scope) {
		final List<File> result = new LinkedList<File>();

		// Add local, non-managed dependencies
		result.addAll(this.dependencies.fileDependencies(scope));

		// Add project dependencies
		result.addAll(this.dependencies.projectDependencies(scope));

		if (jakeIvy == null) {
			return result;
		}

		// Add managed dependencies from Ivy
		final Set<JakeArtifact> artifacts;
		if (module != null) {
			artifacts = jakeIvy.resolve(module, dependencies, scope, parameters);
		} else {
			artifacts = jakeIvy.resolve(dependencies, scope, parameters);
		}
		result.addAll(JakeArtifact.localFiles(artifacts));
		return result;
	}

	/**
	 * Resolves the managed dependencies (dependencies declared as external module).
	 */
	public Set<JakeArtifact> resolveManagedDependencies(JakeScope ... scopes) {
		final Set<JakeScope> scopesSet = new HashSet<JakeScope>();
		for (final JakeScope scope : scopes) {
			scopesSet.add(scope);
			scopesSet.addAll(scope.ancestorScopes());
		}
		final Set<JakeArtifact> result = new HashSet<JakeArtifact>();
		for (final JakeScope scope : scopesSet) {
			if (module != null) {
				result.addAll(jakeIvy.resolve(module, dependencies, scope, parameters));
			} else {
				result.addAll(jakeIvy.resolve(dependencies, scope, parameters));
			}
		}
		return result;
	}

	/**
	 * Gets artifacts belonging to the same module as the specified ones but having the specified scopes.
	 */
	public AttachedArtifacts getAttachedArtifacts(Set<JakeVersionedModule> modules, JakeScope ... scopes) {
		return jakeIvy.getArtifacts(modules, scopes);
	}



	/**
	 * Returns the scopes declared in the underlying dependencies.
	 */
	public Set<JakeScope> declaredScopes() {
		return this.dependencies.moduleScopes();
	}

	/**
	 * Gets the path containing all the artifact files for the specified scopes.
	 */
	public final JakePath get(JakeScope ...scopes) {
		JakePath path = JakePath.of();
		for (final JakeScope scope : scopes) {
			path = path.and(getSingleScope(scope));
		}
		return path.removeDoubloons();
	}

	private final JakePath getSingleScope(JakeScope scope) {
		final JakePath cachedResult = this.cachedDeps.get(scope);
		if (cachedResult != null) {
			return cachedResult;
		}
		JakeLog.startln("Resolving dependencies for scope '" + scope.name() + "'");
		final List<File> list = new LinkedList<File>();
		for (final JakeScope jakeScope : scope.ancestorScopes()) {
			list.addAll(this.getDeclaredDependencies(jakeScope));
		}
		final JakePath result = JakePath.of(list);
		JakeLog.info(result.entries().size() + " artifacts: " + result);
		JakeLog.done();
		cachedDeps.put(scope, result);
		return result;
	}

	/**
	 * Returns <code>true<code> if this resolver does not contain any dependencies.
	 */
	public boolean isEmpty() {
		for (final JakeScope scope : this.declaredScopes()) {
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

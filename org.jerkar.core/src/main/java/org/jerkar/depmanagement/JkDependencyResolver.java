package org.jerkar.depmanagement;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jerkar.JkLog;
import org.jerkar.JkPath;
import org.jerkar.depmanagement.ivy.JkIvy;
import org.jerkar.depmanagement.ivy.JkIvy.AttachedArtifacts;

public final class JkDependencyResolver  {

	public static JkDependencyResolver managed(JkIvy jkIvy, JkDependencies dependencies, JkVersionedModule module, JkResolutionParameters resolutionParameters) {
		return new JkDependencyResolver(jkIvy, dependencies, module, resolutionParameters);
	}

	public static JkDependencyResolver unmanaged(JkDependencies dependencies) {
		if (dependencies.containsExternalModule()) {
			throw new IllegalArgumentException("Your dependencies contain a reference to a managed extarnal module."
					+ "Use #managed method factory instead.");
		}
		return new JkDependencyResolver(null, dependencies, null, null);
	}

	private final Map<JkScope, JkPath> cachedDeps = new HashMap<JkScope, JkPath>();

	private final JkIvy jkIvy;

	private final JkDependencies dependencies;

	private final JkResolutionParameters parameters;

	// Not necessary but nice if present in order to let Ivy hide data efficiently.
	private final JkVersionedModule module;

	private JkDependencyResolver(JkIvy jkIvy, JkDependencies dependencies, JkVersionedModule module, JkResolutionParameters resolutionParameters) {
		this.jkIvy = jkIvy;
		this.dependencies = dependencies;
		this.module = module;
		this.parameters = resolutionParameters;
	}

	public boolean isManagedDependencyResolver() {
		return this.jkIvy != null;
	}

	public JkDependencies declaredDependencies() {
		return this.dependencies;
	}

	private List<File> getDeclaredDependencies(JkScope scope) {
		final List<File> result = new LinkedList<File>();

		// Add local, non-managed dependencies
		result.addAll(this.dependencies.fileDependencies(scope).entries());

		// Add project dependencies
		result.addAll(this.dependencies.projectDependencies(scope));

		if (jkIvy == null) {
			return result;
		}

		// Add managed dependencies from Ivy
		final Set<JkArtifact> artifacts;
		if (module != null) {
			artifacts = jkIvy.resolve(module, dependencies, scope, parameters);
		} else {
			artifacts = jkIvy.resolve(dependencies, scope, parameters);
		}
		result.addAll(JkArtifact.localFiles(artifacts));
		return result;
	}

	/**
	 * Resolves the managed dependencies (dependencies declared as external module).
	 */
	public Set<JkArtifact> resolveManagedDependencies(JkScope ... scopes) {
		if (jkIvy == null) {
			throw new IllegalStateException("This method cannot be invoked on an unmanaged dependency resolver.");
		}
		final Set<JkScope> scopesSet = new HashSet<JkScope>();
		for (final JkScope scope : scopes) {
			scopesSet.add(scope);
			scopesSet.addAll(scope.ancestorScopes());
		}
		final Set<JkArtifact> result = new HashSet<JkArtifact>();
		for (final JkScope scope : scopesSet) {
			if (module != null) {
				result.addAll(jkIvy.resolve(module, dependencies, scope, parameters));
			} else {
				result.addAll(jkIvy.resolve(dependencies, scope, parameters));
			}
		}
		return result;
	}

	/**
	 * Gets artifacts belonging to the same module as the specified ones but having the specified scopes.
	 */
	public AttachedArtifacts getAttachedArtifacts(Set<JkVersionedModule> modules, JkScope ... scopes) {
		return jkIvy.getArtifacts(modules, scopes);
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
		final JkPath cachedResult = this.cachedDeps.get(scope);
		if (cachedResult != null) {
			return cachedResult;
		}
		JkLog.startln("Resolving dependencies for scope '" + scope.name() + "'");
		final List<File> list = new LinkedList<File>();
		for (final JkScope jkScope : scope.ancestorScopes()) {
			list.addAll(this.getDeclaredDependencies(jkScope));
		}
		final JkPath result = JkPath.of(list);
		JkLog.info(result.entries().size() + " artifacts: " + result);
		JkLog.done();
		cachedDeps.put(scope, result);
		return result;
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

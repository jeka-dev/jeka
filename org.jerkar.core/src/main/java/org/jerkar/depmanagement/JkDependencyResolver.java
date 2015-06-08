package org.jerkar.depmanagement;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jerkar.JkClassLoader;
import org.jerkar.JkLog;
import org.jerkar.file.JkPath;
import org.jerkar.internal.ivy.JkIvyPublisher;
import org.jerkar.internal.ivy.JkIvyResolver;
import org.jerkar.internal.ivy.JkIvyResolver.JkAttachedArtifacts;

public final class JkDependencyResolver  {

	private static final String IVY_CLASS = "org.jerkar.internal.ivy.JkIvyResolver";

	private static final JkClassLoader IVY_CLASS_LOADER = JkIvyPublisher.CLASSLOADER;

	private static final boolean IN_CLASSLOADER = JkClassLoader.current().isDefined(IVY_CLASS);

	public static JkDependencyResolver managed(JkRepos repos, JkDependencies dependencies, JkVersionedModule module, JkResolutionParameters resolutionParameters) {
		final Object ivyResolver;
		if (!IN_CLASSLOADER) {
			ivyResolver = IVY_CLASS_LOADER.invokeStaticMethod(false, IVY_CLASS, "of", dependencies, module, resolutionParameters);
		} else {
			ivyResolver = JkIvyResolver.of(repos);
		}
		return new JkDependencyResolver(ivyResolver, dependencies, module, resolutionParameters);
	}

	public static JkDependencyResolver unmanaged(JkDependencies dependencies) {
		if (dependencies.containsExternalModule()) {
			throw new IllegalArgumentException("Your dependencies contain a reference to a managed extarnal module."
					+ "Use #managed method factory instead.");
		}
		return new JkDependencyResolver(null, dependencies, null, null);
	}

	private final Map<JkScope, JkPath> cachedDeps = new HashMap<JkScope, JkPath>();

	private final Object jkIvyResolver;

	private final JkDependencies dependencies;

	private final JkResolutionParameters parameters;

	// Not necessary but nice if present in order to let Ivy hide data efficiently.
	private final JkVersionedModule module;

	private JkDependencyResolver(Object jkIvyResolver, JkDependencies dependencies, JkVersionedModule module, JkResolutionParameters resolutionParameters) {
		this.jkIvyResolver = jkIvyResolver;
		this.dependencies = dependencies;
		this.module = module;
		this.parameters = resolutionParameters;
	}

	public boolean isManagedDependencyResolver() {
		return this.jkIvyResolver != null;
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

		if (jkIvyResolver == null) {
			return result;
		}

		// Add managed dependencies from Ivy
		final Set<JkArtifact> artifacts;
		if (module != null) {
			if (!IN_CLASSLOADER) {
				artifacts = IVY_CLASS_LOADER.invokeInstanceMethod(true, jkIvyResolver, "resolve",
						module, dependencies, scope, parameters);
			} else {
				artifacts = ((JkIvyResolver) jkIvyResolver).resolve(module, dependencies, scope, parameters);
			}

		} else {
			if (!IN_CLASSLOADER) {
				artifacts = IVY_CLASS_LOADER.invokeInstanceMethod(true, jkIvyResolver, "resolveAnonymous",
						dependencies, scope, parameters);
			} else {
				artifacts = ((JkIvyResolver) jkIvyResolver).resolveAnonymous(dependencies, scope, parameters);
			}

		}
		result.addAll(JkArtifact.localFiles(artifacts));
		return result;
	}

	/**
	 * Resolves the managed dependencies (dependencies declared as external module).
	 */
	@SuppressWarnings("unchecked")
	public Set<JkArtifact> resolveManagedDependencies(JkScope ... scopes) {
		if (jkIvyResolver == null) {
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
				if (!IN_CLASSLOADER) {
					result.addAll((Collection<? extends JkArtifact>) IVY_CLASS_LOADER.invokeInstanceMethod(true, jkIvyResolver,
							"resolve", module, dependencies, scope, parameters));
				} else {
					result.addAll(((JkIvyResolver) jkIvyResolver).resolve(module, dependencies, scope, parameters));
				}
			} else {
				if (!IN_CLASSLOADER) {
					result.addAll((Collection<? extends JkArtifact>) IVY_CLASS_LOADER.invokeInstanceMethod(
							true, jkIvyResolver, "resolve", dependencies, scope, parameters));
				} else {
					result.addAll(((JkIvyResolver) jkIvyResolver).resolveAnonymous(dependencies, scope, parameters));
				}
			}
		}
		return result;
	}

	/**
	 * Gets artifacts belonging to the same module as the specified ones but having the specified scopes.
	 */
	public JkAttachedArtifacts getAttachedArtifacts(Set<JkVersionedModule> modules, JkScope ... scopes) {
		if (!IN_CLASSLOADER) {
			return (JkAttachedArtifacts)  IVY_CLASS_LOADER.invokeInstanceMethod(true, jkIvyResolver, "getArtifacts", modules, scopes);
		}
		return ((JkIvyResolver) jkIvyResolver).getArtifacts(modules, scopes);
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

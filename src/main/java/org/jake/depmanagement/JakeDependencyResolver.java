package org.jake.depmanagement;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jake.JakeLog;
import org.jake.JakePath;
import org.jake.depmanagement.JakeDependency.JakeFilesDependency;
import org.jake.depmanagement.ivy.JakeIvy;

public final class JakeDependencyResolver  {

	public static JakeDependencyResolver managed(JakeIvy jakeIvy, JakeDependencies dependencies) {
		return managed(jakeIvy, dependencies, null);
	}

	public static JakeDependencyResolver managed(JakeIvy jakeIvy, JakeDependencies dependencies, JakeVersionedModule module) {
		return new JakeDependencyResolver(jakeIvy, dependencies, module);
	}

	public static JakeDependencyResolver unmanaged(JakeDependencies dependencies) {
		if (dependencies.containsExternalModule()) {
			throw new IllegalArgumentException("Your dependencies contain a reference to a managed extarnal module."
					+ "Use #managed method factory instead.");
		}
		return new JakeDependencyResolver(null, dependencies, null);
	}

	private final Map<JakeScope, JakePath> cachedDeps = new HashMap<JakeScope, JakePath>();

	private final JakeIvy jakeIvy;

	private final JakeDependencies dependencies;

	// Not necessary but nice if present in order to let Ivy hide data efficiently.
	private final JakeVersionedModule module;

	private JakeDependencyResolver(JakeIvy jakeIvy, JakeDependencies dependencies, JakeVersionedModule module) {
		this.jakeIvy = jakeIvy;
		this.dependencies = dependencies;
		this.module = module;
	}

	protected List<File> getDeclaredDependencies(JakeScope scope) {
		final List<File> result = new LinkedList<File>();

		// Add local, non-managed dependencies
		result.addAll(localDependencies(scope));
		if (jakeIvy == null) {
			return result;
		}

		// Add managed dependencies from Ivy
		final Set<JakeArtifact> artefacts;
		if (module != null) {
			artefacts = jakeIvy.resolve(module, dependencies, scope);
		} else {
			artefacts = jakeIvy.resolve(dependencies, scope);
		}
		for (final JakeArtifact artifact : artefacts) {
			result.add(artifact.localFile());
		}
		return result;
	}

	private List<File> localDependencies(JakeScope jakeScope) {
		final LinkedHashSet<File> set = new LinkedHashSet<File>();
		for (final JakeScopedDependency scopedDependency : dependencies) {
			if (scopedDependency.isInvolvedIn(jakeScope)
					&& scopedDependency.dependency() instanceof JakeFilesDependency) {
				final JakeFilesDependency fileDeps = (JakeFilesDependency) scopedDependency.dependency();
				set.addAll(fileDeps.files());
			}
		}
		return new LinkedList<File>(set);
	}

	public Set<JakeScope> declaredScopes() {
		return this.dependencies.moduleScopes();
	}

	public final JakePath get(JakeScope scope) {
		final JakePath cachedResult = this.cachedDeps.get(scope);
		if (cachedResult != null) {
			return cachedResult;
		}
		JakeLog.startAndNextLine("Resolving dependencies for scope '" + scope.name() + "'");
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
		return "JakeIvy: " + jakeIvy + " , dependencies: " + dependencies;
	}

}
